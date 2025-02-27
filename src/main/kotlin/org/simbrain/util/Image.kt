package org.simbrain.util

import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.*
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.floor
import kotlin.math.pow

/**
 * Convert array of float values to array of RGB color values.
 */
fun FloatArray.toSimbrainColor() = map { value ->
    value.clip(-1.0f..1.0f).let {
        if (it < 0) Color.HSBtoRGB(2/3f, -it, 1.0f) else Color.HSBtoRGB(0.0f, it, 1.0f)
    }
}.toIntArray()

/**
 * Convert array of double values to array of RGB color values.
 */
fun DoubleArray.toSimbrainColor() = map { value ->
    value.clip(-1.0..1.0).let {
        if (it < 0) Color.HSBtoRGB(2/3f, (-it).toFloat(), 1.0F) else Color.HSBtoRGB(0.0f, it.toFloat(), 1.0f)
    }
}.toIntArray()

/**
 * Converts a double array to matrix representation (as a Buffered Image) with a specified width and height, in pixels.
 *
 * Width * height must be less than the array size.
 */
fun DoubleArray.toSimbrainColorImage(width: Int, height: Int) = toSimbrainColor().let {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, it.size), null)
    BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a float array to a matrix image as in [DoubleArray.toSimbrainColorImage]
 */
fun FloatArray.toSimbrainColorImage(width: Int, height: Int) = toSimbrainColor().let {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, it.size), null)
    BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a 2d float array to a "square" buffered image.
 */
fun Array<FloatArray>.toSimbrainColorImage() = flattenArray(this).toSimbrainColorImage(first().size, size)

/**
 * Converts a 2d int array to  a "square" buffered image, using 24-bit RGB.
 */
fun IntArray.toRGBImage(width: Int, height: Int): BufferedImage {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(this, size), null)
    return BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a float array to a gray scale image of a specified height and width in pixels.
 *
 * TODO: Does not work with scale command
 */
fun FloatArray.toGrayScaleImage(width: Int, height: Int) = this
        .map { (it.clip(0.0f, 1.0f) * 255).toInt().toByte() }
        .toByteArray()
        .let {
            val colorModel = DirectColorModel(8, 0xff, 0xff, 0xff)
            val sampleModel = colorModel.createCompatibleSampleModel(width, height)
            val raster = Raster.createWritableRaster(sampleModel, DataBufferByte(it, it.size), null)
            BufferedImage(colorModel, raster, false, null)
        }

private val cache = HashMap<Pair<Int, Int>, BufferedImage>()

/**
 * Creates a transparent image with specified pixels.
 */
fun transparentImage(width: Int, height: Int) = cache[width to height] ?: run {
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .also { cache[width to height] = it }
}

/**
 * Convert an array of booleans to a matrix of pixels, transparent for false, and "color" for true.
 */
fun BooleanArray.toOverlay(width: Int, height: Int, color: Color ): BufferedImage {
    val colorModel: ColorModel = DirectColorModel(
        32, 0xff0000, 0x00ff00, 0x0000ff, 0xff shl 24
    )
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val dataBuffer =  this.map { if (it) color.rgb else 0}.toIntArray()
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(dataBuffer, size), null)
    return BufferedImage(colorModel, raster, false, null)
}

/**
 * Scale the size of the image by the provided factor.
 *
 * TODO: Use parent's color model
 */
fun BufferedImage.scale(factor: Double) = BufferedImage(
    floor(width*factor).toInt(),
    floor(height*factor).toInt(),
    BufferedImage.TYPE_INT_ARGB)
    .let {
    AffineTransformOp(AffineTransform().apply { scale(factor, factor) }, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            .filter(this, it)!!
}

/**
 * Rescale the image to a specified height and width in pixels.
 *
 * TODO: Use parent's color model
 */
fun BufferedImage.scale(w: Int, h: Int) = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).let {
    AffineTransformOp(AffineTransform().also { it.scale(w.toDouble() / width, h.toDouble() / height) },
            AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(this, it)!!
}

/**
 * Display an image in a panel.
 */
fun BufferedImage.display() {
    JPanel().apply {
        add(JLabel(ImageIcon(this@display)))
    }.displayInDialog()
}

fun main() {
    val arr = UniformRealDistribution(0.0,1.0).sampleDouble(100)
    val intArray =  UniformIntegerDistribution().apply {
        floor = 0
        ceil = 2.0.pow(24.0).toInt()
    }.sampleInt(100)
    val boolArray = intArray.map { it % 2 == 1 }.toBooleanArray()
    // arr.toSimbrainColorImage(10,10).scale(100, 100).display()
    // arr.toSimbrainColorImage(10,10).scale(20.0).display()
    // intArray.toRGBImage(10,10).scale(10.0).display()
    // arr.toFloatArray().toGrayScaleImage(10,10).display()
    boolArray.toOverlay(10,10, Color.yellow).scale(10.0).display()
}