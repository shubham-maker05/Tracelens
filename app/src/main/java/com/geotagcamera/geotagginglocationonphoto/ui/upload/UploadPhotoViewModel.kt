package com.geotagcamera.geotagginglocationonphoto.ui.upload

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geotagcamera.geotagginglocationonphoto.data.AppDatabase
import com.geotagcamera.geotagginglocationonphoto.data.PhotoEntity
import com.geotagcamera.geotagginglocationonphoto.exif.ExifWriter
import com.geotagcamera.geotagginglocationonphoto.exif.UserCommentCodec
import com.geotagcamera.geotagginglocationonphoto.exif.XmpWriter
import com.geotagcamera.geotagginglocationonphoto.location.GeocoderRepository
import com.geotagcamera.geotagginglocationonphoto.location.LocationFix
import com.geotagcamera.geotagginglocationonphoto.location.LocationProvider
import com.geotagcamera.geotagginglocationonphoto.security.PhotoIntegrity
import com.geotagcamera.geotagginglocationonphoto.stamp.StampPreferences
import com.geotagcamera.geotagginglocationonphoto.stamp.StampRenderer
import com.geotagcamera.geotagginglocationonphoto.storage.MediaStoreImageSaver
import com.geotagcamera.geotagginglocationonphoto.ui.review.ReviewFilename
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class UploadState {
    data object Idle : UploadState()
    data object LocatingAndStamping : UploadState()
    data class Done(val savedUri: Uri) : UploadState()
    data class Error(val message: String) : UploadState()
}

/**
 * "Upload Photo" menu section: pick an existing gallery image, fetch the
 * device's CURRENT location (this stamps where the phone is right now, not
 * wherever the picked photo may have originally been taken — old photos
 * usually carry no GPS EXIF to fall back on), burn a stamp using the exact
 * same [StampRenderer]/[StampPreferences] the camera flow uses, sign it with
 * the same tamper-evident [PhotoIntegrity] pipeline, and publish it into
 * Pictures/TraceLens alongside captured photos.
 */
class UploadPhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val locationProvider = LocationProvider(application)
    private val stampPreferences = StampPreferences(application)
    private val db by lazy { AppDatabase.get(application) }
    private val geocoderRepository by lazy { GeocoderRepository(application, db.geoCacheDao()) }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    fun processPickedImage(uri: Uri) {
        viewModelScope.launch {
            _state.value = UploadState.LocatingAndStamping
            val result = runCatching { stampAndSave(uri) }
            _state.value = result.fold(
                onSuccess = { UploadState.Done(it) },
                onFailure = { UploadState.Error(it.message ?: "Couldn't process that photo.") }
            )
        }
    }

    fun reset() {
        _state.value = UploadState.Idle
    }

    private suspend fun stampAndSave(uri: Uri): Uri = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()

        val bitmap = decodeUpright(uri) ?: error("Couldn't read that image.")

        val fix: LocationFix = locationProvider.getFreshFix()
            ?: error("Couldn't get a location fix — check that location is turned on.")

        val geocode = runCatching { geocoderRepository.reverseGeocode(fix.latitude, fix.longitude) }.getOrNull()
        val fields = stampPreferences.fields.first()
        val capturedAtEpochMs = System.currentTimeMillis()

        val stamped = StampRenderer.stamp(
            context = context,
            source = bitmap,
            fix = fix,
            addressParts = geocode,
            capturedAtEpochMs = capturedAtEpochMs,
            fields = fields
        )
        if (stamped !== bitmap) bitmap.recycle()

        val file = File(context.cacheDir, "upload_stamp_$capturedAtEpochMs.jpg")
        FileOutputStream(file).use { out -> stamped.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        stamped.recycle()

        // Same tamper-evident signing as a fresh capture, so an uploaded photo
        // verifies exactly like a camera one (Verify screen, EXIF UserComment, XMP).
        val integrity = PhotoIntegrity.sign(file)
        val proof = UserCommentCodec.encode(integrity, capturedAtEpochMs)
        ExifWriter.write(file, fix, capturedAtEpochMs, proof)
        XmpWriter.write(file, proof)

        val filename = ReviewFilename.generate(geocode?.place, capturedAtEpochMs)
        val savedUri = MediaStoreImageSaver.save(context, file, filename)
            ?: error("Couldn't save the stamped photo.")
        file.delete()

        db.photoDao().insert(
            PhotoEntity(
                filePath = savedUri.toString(),
                capturedAtEpochMs = capturedAtEpochMs,
                latitude = fix.latitude,
                longitude = fix.longitude,
                altitudeMeters = fix.altitudeMeters,
                accuracyMeters = fix.accuracyMeters,
                bearingDegrees = fix.bearingDegrees,
                address = geocode?.addressLine,
                addressFromCache = geocode?.fromCache ?: false,
                orgLabel = fields.orgLabel.ifBlank { null },
                sha256Hash = integrity.sha256Hex,
                signatureBase64 = integrity.signatureBase64,
                signingKeyAlias = integrity.keyAlias,
                fieldWorkerSignature = false
            )
        )

        savedUri
    }

    /** Rotates the picked image upright per its own EXIF orientation, so the stamp burns in the right way up regardless of how the source photo was saved. */
    private fun decodeUpright(uri: Uri): Bitmap? {
        val context = getApplication<Application>()
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        if (rotationDegrees == 0) return original
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        if (rotated !== original) original.recycle()
        return rotated
    }
}
