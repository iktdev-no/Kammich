package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceAbility

class GPhoto2AbilityParser: GPhoto2Parser<GPhoto2DeviceAbility> {
    override fun parse(input: String): GPhoto2DeviceAbility {
        fun getValue(key: String): String = input.lines()
            .find { it.contains(key) }
            ?.substringAfter(":")?.trim() ?: ""

        // Henter ut Capture Choices spesielt siden den kan gå over flere linjer
        val captureSection = input.substringAfter("Capture choices")
            .substringBefore("Configuration support")
            .lines()
            .map { it.trim().removePrefix(":").trim() }
            .filter { it.isNotEmpty() }

        return GPhoto2DeviceAbility(
            camera = getValue("Abilities for camera"),
            serialPortSupport = getValue("Serial port support") == "yes",
            usbSupport = getValue("USB support") == "yes",
            captureChoices = captureSection,
            configurationSupport = getValue("Configuration support") == "yes",
            deleteSelectedFiles = getValue("Delete selected files on camera") == "yes",
            deleteAllFiles = getValue("Delete all files on camera") == "yes",
            filePreviewSupport = getValue("File preview (thumbnail) support") == "yes",
            fileUploadSupport = getValue("File upload support") == "yes"
        )
    }
}