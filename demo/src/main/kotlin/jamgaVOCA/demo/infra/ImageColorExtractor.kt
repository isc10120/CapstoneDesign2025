package jamgaVOCA.demo.infra

import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.net.URL
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

@Component
class ImageColorExtractor {

    fun extractFromUrl(imageUrl: String): String {
        val bytes = URL(imageUrl).readBytes()
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        return extractFromImage(image)
    }

    fun extract(base64: String): String {
        val bytes = Base64.getDecoder().decode(base64)
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        return extractFromImage(image)
    }

    private fun extractFromImage(image: BufferedImage): String {
        val colorCount = mutableMapOf<Int, Int>()

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getRGB(x, y)
                val alpha = (rgb shr 24) and 0xFF
                if (alpha < 128) continue

                val r = ((rgb shr 16) and 0xFF) / 32 * 32
                val g = ((rgb shr 8) and 0xFF) / 32 * 32
                val b = (rgb and 0xFF) / 32 * 32
                val grouped = (r shl 16) or (g shl 8) or b

                colorCount[grouped] = (colorCount[grouped] ?: 0) + 1
            }
        }

        val dominant = colorCount.maxByOrNull { it.value }?.key ?: 0
        val r = (dominant shr 16) and 0xFF
        val g = (dominant shr 8) and 0xFF
        val b = dominant and 0xFF
        return String.format("#%02X%02X%02X", r, g, b)
    }
}