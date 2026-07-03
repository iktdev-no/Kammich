package no.iktdev.kammich.gphoto2.model

data class GPhoto2DeviceAbility(
    val camera: String,
    val serialPortSupport: Boolean,
    val usbSupport: Boolean,
    val captureChoices: List<String>,
    val configurationSupport: Boolean,
    val deleteSelectedFiles: Boolean,
    val deleteAllFiles: Boolean,
    val filePreviewSupport: Boolean,
    val fileUploadSupport: Boolean
)