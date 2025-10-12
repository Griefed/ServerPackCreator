/*
 * MIT License
 *
 * Copyright (c) 2022 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package de.griefed.serverpackcreator.app.gui.window.control.components

import de.griefed.serverpackcreator.app.gui.window.control.components.LarsonScanner.ScannerConfig.Companion.HIGH
import de.griefed.serverpackcreator.app.gui.window.control.components.LarsonScanner.ScannerConfig.Companion.LOW
import de.griefed.serverpackcreator.app.gui.window.control.components.LarsonScanner.ScannerConfig.Companion.MEDIUM
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.floor
import kotlin.math.roundToInt


/**
 * A Larson Scanner which some may or may not know from the Cylons from Battlestar Galactica or Kitt
 * from Knight Rider.
 *
 *
 * The Larson Scanner is named after Glen A. Larson, who produced the Battlestar Galactica and
 * Knight Rider series, and is responsible for introducing scanning red light effects to the sci-fi
 * TV viewing population of the 1970s and 80s.
 *
 *
 * Basically, a Larson Scanner is a red light which runs from left to right, to left, to right
 * etc. with, usually, a nice fading effect. It's so simple, but oh so awesome!
 *
 *
 * A user by the name of [Kreezxil](https://github.com/kreezxil) made a [request](https://github.com/Griefed/ServerPackCreator/issues/338) for [ServerPackCreator](https://github.com/Griefed/ServerPackCreator) to have some sort of
 * progress-/thinking bar to indicate that ServerPackCreator is currently busy doing server pack
 * things. And off I went to create this little over engineered thing. Enjoy!
 *
 * @author Griefed
 */
@Suppress("unused")
class LarsonScanner : JPanel {
    private val eye: Eye

    /**
     * Create a Larson Scanner with default settings.
     *
     * Default settings are:
     *
     *  * Scanner background: Black
     *  * Timer interval: 100ms
     *  * Eye colour: Red
     *  * Fractions: 0.4f, 1.0f
     *  * Alphas: 100,200,255,200,100
     *  * Divider: 25
     *  * Number of elements: 5
     *  * Aspect ratio forced: false
     *  * Shape: Oval
     *  * Gradient colours: true
     *  * Divider used: true
     *  * Rendering quality: Low
     *
     * @author Griefed
     */
    constructor() : super() {
        isDoubleBuffered = false
        layout = BorderLayout()
        background = DEFAULT_BACKGROUND_COLOUR
        eye = Eye()
        add(eye, BorderLayout.CENTER)
        eye.run()
    }

    /**
     * Convenience constructor allowing you to specify the interval at which the position changes. For
     * more information regarding possible settings, see [LarsonScanner].
     *
     * @param updateInterval Interval in milliseconds at which to scroll.
     * @author Griefed
     */
    constructor(updateInterval: Short) : super() {
        isDoubleBuffered = true
        layout = BorderLayout()
        background = DEFAULT_BACKGROUND_COLOUR
        eye = Eye(updateInterval)
        add(eye, BorderLayout.CENTER)
        eye.run()
    }

    /**
     * Convenience constructor allowing you to specify the interval at which the position changes, as
     * well as the background colours for the scanner and the eye. For more information regarding
     * possible settings, see [LarsonScanner].
     *
     * @param interval Interval in milliseconds at which to scroll.
     * @param backgroundColor The background colour for the scanner and the eye.
     * @author Griefed
     */
    constructor(interval: Short, backgroundColor: Color) : super() {
        isDoubleBuffered = true
        layout = BorderLayout()
        background = backgroundColor
        eye = Eye(interval, backgroundColor)
        add(eye, BorderLayout.CENTER)
        eye.run()
    }

    /**
     * Convenience constructor allowing you to specify the interval at which the position changes, the
     * background colours for the scanner and the eye, and the color of the eye. For more information
     * regarding possible settings, see [LarsonScanner].
     *
     * @param interval Interval in milliseconds at which to scroll.
     * @param backgroundColor The background colour for the scanner and the eye.
     * @param eyeColor The color of the eye.
     * @author Griefed
     */
    constructor(interval: Short, backgroundColor: Color, eyeColor: Color) : super() {
        isDoubleBuffered = true
        layout = BorderLayout()
        background = backgroundColor
        eye = Eye(interval, backgroundColor, eyeColor)
        add(eye, BorderLayout.CENTER)
        eye.run()
    }

    /**
     * The number of elements in the eye. This number must be odd, meaning that it must not be
     * divisible by two. Examples: 1, 3, 5, 7, 9, 11, 13, 15, 17 and so on.
     *
     * Default setting: `5`
     *
     * @throws IllegalArgumentException if the number specified is smaller than 1 or an even number.
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var numberOfElements: Byte
        get() = eye.numberOfElements
        set(amount) {
            require(amount >= 1) { "Number of elements must be greater than zero. Specified $amount" }
            require(amount % 2 != 0) { "Number of elements must be an odd number. Specified $amount" }
            pause()
            eye.numberOfElements = amount
            val newColours = arrayOfNulls<Color>(amount.toInt())
            for (i in 0 until amount) {
                if (i < eye.eyeColours.size) {
                    newColours[i] = eye.eyeColours[i]
                } else {
                    newColours[i] = DEFAULT_EYE_COLOUR
                }
            }
            eye.eyeColours = newColours.filterNotNull().toTypedArray()
            val newAlphas = ShortArray(amount.toInt())
            val median = (amount + 1) / 2
            for (i in 0 until amount) {
                if (i + 1 < median) {
                    newAlphas[i] = (255 / median * (i + 1)).toShort()
                } else if (i + 1 == median) {
                    newAlphas[i] = 255
                } else {
                    newAlphas[i] = ((amount - i) * 255 / median).toShort()
                }
            }
            eye.alphas = newAlphas
            play()
        }

    /**
     * Set the colour for all elements in the eye to the same colour.
     *
     * Default setting: `255,0,0`
     *
     * @param color The color to set all elements in the eye to.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setEyeColour(color: Color) {
        eye.setEyeColour(color)
    }

    /**
     * The colour for each element in the eye, from left to right. This [Color]-array must
     * be the same size as the current number of elements in the eye.
     *
     * Default setting: `{255,0,0 , 255,0,0 , 255,0,0 , 255,0,0 , 255,0,0}`
     *
     * @throws IllegalArgumentException if the size of the array is unequal to the current number of
     * elements in the eye.
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var eyeColours: Array<Color>
        get() = eye.eyeColours
        set(value) {
            eye.eyeColours = value
        }

    /**
     * Background colour for the eye.
     *
     * Default setting: `0,0,0`
     * @author Griefed
     */
    var eyeBackground: Color
        get() = eye.background
        set(backgroundColor) {
            eye.background = backgroundColor
        }

    /**
     * Whether to draw each element oval or rectangular. Set to `true` to use oval-shapes,
     * `false` to use rectangular shapes.
     *
     * Default setting: `true`
     *
     * @param useOval Whether to use oval or rectangular shapes.
     * @author Griefed
     */
    fun drawOval(useOval: Boolean) {
        eye.ovalShaped = useOval
    }

    /**
     * Toggle the eye between oval-shape and rectangle-shape.
     *
     * @author Griefed
     */
    fun toggleShape() {
        eye.ovalShaped = !eye.ovalShaped
    }

    /**
     * Whether the shape is currently set to oval.
     *
     * @return `true` if oval, false otherwise.
     * @author Griefed
     */
    val isShapeOval: Boolean
        get() = eye.ovalShaped

    /**
     * Whether to draw the eye using gradients. Set to `true` to use gradients, `false`
     * to use solid colours. Depending on the shape, gradients are drawn in two ways:
     *
     *
     *  * Oval shapes: Gradients are created from the center of element `n` to the
     * height of element `n`. This is most beautiful when forcing the aspect ratio,
     * as radial gradients are best for circles, not ovals.
     *  * Rectangle shapes: Gradients are created from left to right, at half the height of element
     * `n`, over the width of element `n`, for each element.
     *
     * Default setting: `true`
     *
     * @param useGradient Whether to use gradients or solid colours.
     * @author Griefed
     */
    fun useGradient(useGradient: Boolean) {
        eye.useGradients = useGradient
    }

    /**
     * Toggle the drawing of the eye between gradient and solid colours. For more information, see
     * [useGradient].
     *
     * @author Griefed
     */
    fun toggleGradient() {
        eye.useGradients = !eye.useGradients
    }

    /**
     * Whether the eye is currently being drawn using gradients or solid colours. For more
     * information, see [useGradient].
     *
     * @return `true` when the eye is drawn using gradients.
     * @author Griefed
     */
    val isGradientActive: Boolean
        get() = eye.useGradients

    /**
     * Whether to use a divider to in-/decrement across the width of the Larson Scanner. If the
     * divider is being used, then the position of the eye will be updated with the result of the
     * width of the Scanner divided by the divider. Meaning: Smaller values increase the speed of the
     * eye whilst bigger values decrease it.
     *
     * Increment: next position = current position + width of the Scanner / divider
     * Decrement: next position = current position - width of the Scanner / divider
     *
     * Default setting: `true`
     *
     * @param useDivider Whether to in-/decrement using the divider. `true`
     * to use it.
     * @author Griefed
     */
    fun useDivider(useDivider: Boolean) {
        eye.useDivider = useDivider
    }

    /**
     * Toggle the use of the divider. See [useDivider] for more information about the
     * divider and how it works.
     *
     * @author Griefed
     */
    fun toggleDivider() {
        eye.useDivider = !eye.useDivider
    }

    /**
     * Whether the divider is currently being used. See [useDivider] for more
     * information about the divider and how it works.
     *
     * @return `true` if the divider is being used.
     * @author Griefed
     */
    val isDividerActive: Boolean
        get() = eye.useDivider

    /**
     * Whether to use the Cylon-eye animation or Kitt-eye animation. If the Cylon-animation is chosen,
     * elements in the eye expand from the center outward. If the Kitt-animation is chosen, the eye
     * behaves as follows:
     *
     *  * Left-to-right:
     *
     *  * From the current position towards the right, each element is drawn. Each element is
     * drawn with less alpha, leading to the last element, visually the first of the eye,
     * being or almost being solid.
     *  * When the eye leaves the left side, the illusion of the eye emerging is created by
     * drawing the left most element of the elements already drawn from 0 to the width of
     * the eye. As the eye leaves the play, the starting element increases in alpha, thus
     * creating the illusion of the eye emerging from one spot.
     *  * When the eye enters the right side, the illusion of the elements gathering is
     * created by drawing the brightest, most solid, element at the absolute
     * right, thus creating the illusion of the elements coalescing.
     *
     *  * Right-to-left:
     *
     *  * From the current position towards the left, each element is drawn.Each element is
     * drawn with less alpha, leading to the last element, visually the first or the eye,
     * being or almost being solid.
     *  * When the eye leaves the right side, the illusion of the eye emerging is created by
     * drawing the right most element of the elements already drawn from the width of the
     * Larson Scanner to the width of the eye. As the eye leaves the end, the last element
     * increases in alpha, thus creating the illusion of the eye emerging from one spot.
     *  * When the eye enters the left side, the illusion of the elements gathering is
     * created by drawing the brightest, most solid, element at the absolute
     * left, thus creating the illusion of the elements coalescing.
     *
     * @author Griefed
     * @param useCylonAnimation `true` to use the Cylon animation.
     */
    fun useCylonAnimation(useCylonAnimation: Boolean) {
        eye.cylonAnimation = useCylonAnimation
    }

    /**
     * Toggle whether the eye os to be animated using the Cylon-eye or Kitt-eye animation. For more
     * information, see [LarsonScanner.useCylonAnimation].
     *
     * @author Griefed
     */
    fun toggleCylonAnimation() {
        eye.cylonAnimation = !eye.cylonAnimation
    }

    /**
     * Whether the eye is currently being animated using the Cylon-eye or Kitt-eye animation. For
     * more information, see [LarsonScanner.useCylonAnimation].
     *
     * @author Griefed
     * @return `true` if the eye is being animated as a Cylon-eye.
     */
    val isCylonAnimation: Boolean
        get() = eye.cylonAnimation

    /**
     * Set the enforcement of an aspect ratio. If the aspect ratio is being forced, each element will
     * be drawn with a 1:1 aspect ratio, turning ovals into circles and rectangles into squares. The
     * aspect ratio is being enforced by de-/increasing the height of the eye in the scanner to the
     * width of one element, resulting in a nice and sexy 1:1 ratio.
     *
     *
     * If you plan on using different scanner and eye background colours, you should make use of
     * this setting, otherwise the eye background colour will also prevail.
     *
     * Default setting: `false`
     *
     * @param force
     * @author Griefed
     */
    fun forceAspectRatio(force: Boolean) {
        eye.forceAspectRatio = force
    }

    /**
     * Toggle the enforcement of an aspect ratio. See [forceAspectRatio] for more
     * information about an aspect ratio enforcement.
     *
     * @author Griefed
     */
    fun toggleAspectRatio() {
        eye.forceAspectRatio = !eye.forceAspectRatio
    }

    /**
     * Whether the aspect ratio is currently being enforced. See [forceAspectRatio]
     * for more information about an aspect ratio enforcement.
     *
     * @return `true` if the aspect ratio is being enforced.
     * @author Griefed
     */
    val isAspectRatioForced: Boolean
        get() = eye.forceAspectRatio

    /**
     * Set the fractions for the distribution of the colours along the gradients of each oval-shaped
     * element. Keep in mind, that the
     *
     *  1. fractions **must** range from 0.0f to 1.0f.
     *  1. first fraction **must** be smaller than the second one.
     *  1. first fraction **must not** be the same size as the second one
     *  1. first fraction **can** be 0.0f, but not smaller
     *  1. second fraction **must** be bigger than the first one
     *  1. second fraction **must not** be 0.0f
     *
     * Default setting: `0.4f, 1.0f`
     *
     * @param fractionOne Fraction to distribute colour across a radial gradient.
     * Ranging from 0.0f to 1.0f, smaller than `fractionTwo`.
     * @param fractionTwo Fraction to distribute colour across a radial gradient.
     * Ranging from 0.0f to 1.0f, bigger than `fractionOne`.
     * @throws IllegalArgumentException if fractionOne is smaller than 0.0f, if fractionOne is bigger
     * or equal to 1.0f, if fractionOne is bigger than fractionTwo, or if fractionTwo is bigger
     * than 1.0f.
     * @author Griefed
     */
    @Throws(IllegalArgumentException::class)
    fun setFractions(fractionOne: Float, fractionTwo: Float) {
        if (fractionOne < 0.0f) {
            throw IllegalArgumentException(
                "First fraction must not be negative. Specified $fractionOne"
            )

        } else if (fractionOne >= 1.0f) {
            throw IllegalArgumentException(
                "First fraction must be smaller than 1.0f. Specified $fractionOne"
            )

        } else if (fractionOne > fractionTwo) {
            throw IllegalArgumentException(
                "First fraction must be smaller than fraction two. Specified $fractionOne"
            )

        } else if (fractionTwo > 1.0f) {
            throw IllegalArgumentException(
                "Second fraction must be bigger than the first, and smaller or equal to 1.0f. Specified $fractionTwo"
            )

        } else {
            eye.fractions[0] = fractionOne
            eye.fractions[1] = fractionTwo
        }

    }

    /**
     * The currently set fractions for use in radial gradient creation. See [setFractions] for more information.
     *
     * @return Two fractions currently set.
     * @author Griefed
     */
    val fractions: FloatArray
        get() = eye.fractions

    /**
     * The alpha values for the elements in the eye, one for each element. The array must contain
     * exactly as many entries as there are number of elements currently being drawn in the eye.
     *
     * <Strong>Note:</Strong>
     *
     * Alpha values are reset / automatically set / calculated when you change the number of
     * elements in your configuration. So, if you plan on using custom alpha values and a custom
     * amount of elements, make sure to update your alphas after changing the number of elements!
     *
     * Depending on the currently set shape, these alpha values are used in different ways:
     *
     *  * Oval: Radial gradients are generated, one for each element in the eye. The alpha is
     * applied to the colour in said gradients, from the center of the element to the edge, with
     * the alpha continually increasing. Outer elements frequently end up fading into
     * transparency, especially if larger number of elements are being drawn.
     *  * Rectangular: Linear gradients are generated, one for each element in the eye. The alpha
     * is applied from left-to-right at half the height of each element, for each element. For
     * each element left-to-center the alpha decreases gradually. For each element
     * center-to-right the alpha continually increases.
     *
     * Default setting: `100, 200, 255, 200, 100`
     *
     * @throws IllegalArgumentException if the amount of alphas is unequal to the current number of
     * elements present in the eye.
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var alphas: ShortArray
        get() = eye.alphas
        set(alphas) {
            require(alphas.size == eye.numberOfElements.toInt()) {
                ("Alpha-array must contain exactly " + eye.numberOfElements + " entries. Specified " + alphas.size)
            }
            eye.alphas = alphas
        }

    /**
     * Interval in milliseconds at which to fire the timer of the eye. Every `n`
     * milliseconds the position of the eye gets updated and the eye redrawn. Smaller values therefor
     * increase the speed at which the eye scrolls across the screen, whilst bigger values decrease it.
     *
     * Default setting: `100`
     *
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var interval: Short
        get() = eye.interval
        set(updateInterval) {
            require(updateInterval >= 1) { "Interval must be greater than 0. Specified $updateInterval" }
            eye.interval = updateInterval
        }

    /**
     * Divider with which the position of the eye is being in-/decremented. For more
     * information on how the divider affects the eye, see [useDivider].
     *
     *
     * Default setting: `25`
     *
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var divider: Short
        get() = eye.divider
        set(newStepDivider) {
            require(newStepDivider >= 1) { "Divider must be greater than 0. Specified $newStepDivider" }
            eye.divider = newStepDivider
        }

    /**
     * Width of the gap between rectangular shapes in percent. The gap between rectangular
     * shapes is `n%` of the width of one element.
     *
     * Default setting: `25.0f`
     *
     * @throws IllegalArgumentException if the specified percentage is smaller than 0.0f.
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var gapPercent: Float
        get() = eye.gapPercent
        set(percentile) {
            require(!(percentile < 0.0f)) { "Gap percent must be a positive, non-negative, number. Specified $percentile" }
            eye.gapPercent = percentile
            eye.setNewEyeValues()
        }

    /**
     * The partition divider controls the width of the eye in the Larson Scanner. The total width of
     * the available area is divided by this value, and the resulting value is the total width of the
     * eye in which all elements will be drawn. The smaller the value, the bigger the eye and the
     * other way around.
     *
     * @throws IllegalArgumentException if the specified divider is smaller than or equal to 0.0D.
     * @author Griefed
     */
    @set:Throws(IllegalArgumentException::class)
    var partitionDivider: Double
        get() = eye.partitionDivider
        set(partitionDivider) {
            require(!(partitionDivider <= 0.0)) { "Partition Divider must be bigger than 0.0D. Specified $partitionDivider" }
            eye.partitionDivider = partitionDivider
            eye.setNewEyeValues()
        }

    /**
     * Pause the eye, freezing the animation.
     *
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun pause() {
        eye.pauseAnimation()
    }

    /**
     * Unpause the eye, continuing the animation.
     *
     * @author Griefed
     */
    fun play() {
        eye.playAnimation()
    }

    /**
     * Whether the eye is currently being animated. `false` if the eye is stopped.
     *
     * @author Griefed
     */
    val isRunning: Boolean
        get() = eye.isRunning

    /**
     * Toggle the animation of the eye on/off.
     *
     * @author Griefed
     */
    fun togglePauseUnpause() {
        eye.togglePauseUnpause()
    }

    /**
     * Last set rendering quality. See [setQualityLow], [setQualityMedium] and [setQualityHigh] for more information.
     *
     * @author Griefed
     */
    val qualitySetting: Int
        get() = eye.lastSetRenderingQuality

    /**
     * Set the rendering quality of the eye to high. Settings are:
     *
     *  1. RenderingHints.KEY_ANTIALIASING<br></br>
     * RenderingHints.VALUE_ANTIALIAS_ON
     *  1. RenderingHints.KEY_ALPHA_INTERPOLATION<br></br>
     * RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
     *  1. RenderingHints.KEY_COLOR_RENDERING<br></br>
     * RenderingHints.VALUE_COLOR_RENDER_QUALITY
     *  1. RenderingHints.KEY_INTERPOLATION<br></br>
     * RenderingHints.VALUE_INTERPOLATION_BICUBIC
     *  1. RenderingHints.KEY_RENDERING<br></br>
     * RenderingHints.VALUE_RENDER_QUALITY
     *
     * Default setting: `low`
     *
     * @author Griefed
     */
    fun setQualityHigh() {
        eye.setRenderingQualityHigh()
    }

    /**
     * Set the rendering quality of the eye to medium. Settings are:
     *
     *  1. RenderingHints.KEY_ANTIALIASING<br></br>
     * RenderingHints.VALUE_ANTIALIAS_DEFAULT
     *  1. RenderingHints.KEY_ALPHA_INTERPOLATION<br></br>
     * RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT
     *  1. RenderingHints.KEY_COLOR_RENDERING<br></br>
     * RenderingHints.VALUE_COLOR_RENDER_DEFAULT
     *  1. RenderingHints.KEY_INTERPOLATION<br></br>
     * RenderingHints.VALUE_INTERPOLATION_BILINEAR
     *  1. RenderingHints.KEY_RENDERING<br></br>
     * RenderingHints.VALUE_RENDER_DEFAULT
     *
     * Default setting: `low`
     *
     * @author Griefed
     */
    fun setQualityMedium() {
        eye.setRenderingQualityMedium()
    }

    /**
     * Set the rendering quality of the eye to low. Settings are:
     *
     *  1. RenderingHints.KEY_ANTIALIASING<br></br>
     * RenderingHints.VALUE_ANTIALIAS_OFF
     *  1. RenderingHints.KEY_ALPHA_INTERPOLATION<br></br>
     * RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
     *  1. RenderingHints.KEY_COLOR_RENDERING<br></br>
     * RenderingHints.VALUE_COLOR_RENDER_SPEED
     *  1. RenderingHints.KEY_INTERPOLATION<br></br>
     * RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
     *  1. RenderingHints.KEY_RENDERING<br></br>
     * RenderingHints.VALUE_RENDER_SPEED
     *
     * Default setting: `low`
     *
     * @author Griefed
     */
    fun setQualityLow() {
        eye.setRenderingQualityLow()
    }

    /**
     * Set the configuration of the Larson Scanner with values from the given config.
     *
     * @param config The configuration from which to set the Larson Scanner values.
     * @throws IllegalArgumentException if any of the configured values is invalid.
     * @author Griefed
     */
    @Throws(IllegalArgumentException::class)
    fun loadConfig(config: ScannerConfig) {
        when (config.qualitySetting) {
            0 -> {
                setQualityLow()
            }

            1 -> {
                setQualityMedium()
            }

            else -> {
                setQualityHigh()
            }
        }
        numberOfElements = config.numberOfElements
        alphas = config.alphas
        eyeColours = config.eyeColours
        interval = config.interval
        divider = config.divider
        setFractions(config.fractions[0], config.fractions[1])
        gapPercent = config.gapPercent
        partitionDivider = config.partitionDivider
        forceAspectRatio(config.isAspectRatioForced)
        drawOval(config.isShapeOval)
        useGradient(config.isGradientActive)
        useDivider(config.isDividerActive)
        useCylonAnimation(config.isCylonAnimation)
        background = config.scannerBackgroundColour
        eye.background = config.eyeBackgroundColour
    }

    /**
     * Load the default values into the Larson Scanner and eye, resetting it. See [LarsonScanner] for more information on what the defaults are.
     *
     * @author Griefed
     */
    fun loadDefaults() {
        setQualityLow()
        numberOfElements = 5.toByte()
        alphas = shortArrayOf(100, 200, 255, 200, 100)
        setEyeColour(DEFAULT_EYE_COLOUR)
        interval = 100.toShort()
        divider = 25.toShort()
        setFractions(0.4f, 1.0f)
        gapPercent = 25.0f
        partitionDivider = 5.0
        forceAspectRatio(false)
        drawOval(true)
        useGradient(true)
        useDivider(true)
        useCylonAnimation(true)
        background = DEFAULT_BACKGROUND_COLOUR
        eye.background = DEFAULT_BACKGROUND_COLOUR
    }

    /**
     * Current Larson Scanner configuration as a ScannerConfig.
     *
     * @author Griefed
     */
    val currentConfig: ScannerConfig
        get() = ScannerConfig(
            eye.lastSetRenderingQuality,
            eye.alphas,
            eye.interval,
            eye.divider,
            eye.numberOfElements,
            eye.fractions,
            eye.gapPercent,
            eye.partitionDivider,
            eye.forceAspectRatio,
            eye.ovalShaped,
            eye.useGradients,
            eye.useDivider,
            eye.cylonAnimation,
            eye.eyeColours,
            background,
            eye.background
        )

    /**
     * Convenience-class with which to change or acquire the configuration of the LarsonScanner.
     *
     * @author Griefed
     */
    class ScannerConfig {
        val fractions = floatArrayOf(0.4f, 1.0f)

        /**
         * The colours for each element in the eye for this configuration. For more information, see
         * [LarsonScanner.eyeColours].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var eyeColours = arrayOf(
            DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR
        )
            set(value) {
                field = if (value.size != numberOfElements.toInt()) {
                    throw IllegalArgumentException(
                        "Color-array must contain exactly $numberOfElements entries. Specified ${value.size}"
                    )
                } else {
                    value
                }
            }

        /**
         * This configurations' background colour for the Larson Scanner.
         *
         * @author Griefed
         */
        var scannerBackgroundColour = DEFAULT_BACKGROUND_COLOUR

        /**
         * Background colour for the eye of the Larson Scanner of this configuration.
         *
         * @author Griefed
         */
        var eyeBackgroundColour = DEFAULT_BACKGROUND_COLOUR

        /**
         * This configurations' gap percentage. For more information, see [LarsonScanner.gapPercent].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var gapPercent = 25.0f
            set(gapPercent) {
                field = if (gapPercent < 0.0f) {
                    throw IllegalArgumentException(
                        "Gap percent must be a positive, non-negative, number. Specified $gapPercent"
                    )
                } else {
                    gapPercent
                }
            }

        /**
         * Set this configurations' rendering quality setting. Either [LOW], [MEDIUM] or
         * [HIGH]. For more information, see [LarsonScanner.setQualityLow].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var qualitySetting = LOW
            set(qualitySetting) {
                field = if (qualitySetting < 0 || qualitySetting > 2) {
                    throw IllegalArgumentException(
                        "Quality setting must be 0, 1 or 2. Specified $qualitySetting"
                    )
                } else {
                    qualitySetting
                }
            }

        /**
         * Alpha-array. For more information, see [LarsonScanner.alphas].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var alphas = shortArrayOf(100, 200, 255, 200, 100)
            set(alphas) {
                field = if (alphas.size != numberOfElements.toInt()) {
                    throw IllegalArgumentException(
                        "Alpha-array must contain exactly $numberOfElements entries. Specified ${alphas.size}"
                    )
                } else {
                    alphas
                }
            }

        /**
         * This configurations' interval. For more information, see [LarsonScanner.interval].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var interval: Short = 100
            set(intervalInMillis) {
                field = if (intervalInMillis < 1) {
                    throw IllegalArgumentException(
                        "Interval must be greater than 0. Specified $intervalInMillis"
                    )
                } else {
                    intervalInMillis
                }
            }

        /**
         * This configurations' divider. For more information on how the divider affects the eye,
         * see [LarsonScanner.useDivider].
         *
         * @author Griefed
         */
        @set:Throws(IllegalArgumentException::class)
        var divider: Short = 25
            set(divider) {
                field = if (divider < 1) {
                    throw IllegalArgumentException("Divider must be greater than 0. Specified $divider")
                } else {
                    divider
                }
            }

        /**
         * Number of elements in th eye. For more information, see [LarsonScanner.numberOfElements].
         *
         * @return This configurations' number of elements in the eye.
         * @author Griefed
         */
        var numberOfElements: Byte = 5
            set(value) {
                require(value >= 1) { "Number of elements must be greater than zero. Specified $value" }
                require(value.toInt() % 2 != 0) { "Number of elements must be an odd number. Specified $value" }
                field = value
            }

        /**
         * This configurations' partition divider. For more information, see [LarsonScanner.partitionDivider].
         *
         * @throws IllegalArgumentException if the specified divider is smaller than or equal to 0.0D.
         */
        @set:Throws(IllegalArgumentException::class)
        var partitionDivider = 5.0
            set(partitionDivider) {
                field = if (partitionDivider <= 0.0) {
                    throw IllegalArgumentException(
                        "Partition Divider must be bigger than 0.0D. Specified $partitionDivider"
                    )
                } else {
                    partitionDivider
                }
            }

        /**
         * Whether the aspect ratio is enforced in this configuration.
         *
         * @return Whether the aspect ratio is enforced in this configuration.
         * @author Griefed
         */
        var isAspectRatioForced = false
            private set

        /**
         * Whether the elements for this configuration are drawn as ovals or rectangles.
         *
         * @return Whether elements are drawn as ovals or rectangles.
         * @author Griefed
         */
        var isShapeOval = true
            private set

        /**
         * Whether the elements for this configuration are drawn using gradients.
         *
         * @return Whether elements are drawn using gradients.
         * @author Griefed
         */
        var isGradientActive = true
            private set

        /**
         * Whether the position of this configuration in-/decrements using a divider.
         *
         * @return Whether the position in-/decrements using a divider.
         * @author Griefed
         */
        var isDividerActive = true
            private set

        /**
         * Whether this configuration should animate the eye as a Cylon-eye, or Kitt-eye. For more
         * information, see [LarsonScanner.useCylonAnimation].
         *
         * @author Griefed
         */
        var isCylonAnimation = true

        /**
         * Create a Larson Scanner configuration with default values.
         *
         * @author Griefed
         */
        constructor()

        /**
         * Create a Larson Scanner configuration with custom settings.
         *
         * @param qualitySetting The quality preset to use. Either [LOW], [MEDIUM] or [HIGH]. For more information,
         * see [LarsonScanner.setQualityLow]. [LarsonScanner.setQualityMedium] and [LarsonScanner.setQualityHigh].
         * @param alphaValues Alpha values for the elements in the eye. Length of
         * array must equal number of elements in the eye. For more information, see [alphas].
         * @param intervalInMillis The interval in milliseconds at which to update and
         * redraw the eye. For more information, see [interval].
         * @param divider The divider with which to in-/decrement the position of the eye.
         * For more information, see [LarsonScanner.useGradient].
         * @param numberOfElements The number of elements to draw in the eye. For more
         * information, see [LarsonScanner.numberOfElements].
         * @param fractions Fractions to use for radial gradient colour
         * distribution. For more information, see [LarsonScanner.setFractions].
         * @param gapPercent The percentage of the width of the gap between two
         * rectangular elements. For more information, see [gapPercent].
         * @param partitionDivider The number with which to divide the width of the
         * Larson Scanner to set the width of the eye. For more information, see [partitionDivider].
         * @param forceAspectRatio Whether to force an aspect ratio on the eye. For more
         * information, see [LarsonScanner.forceAspectRatio].
         * @param ovalShaped Whether to draw the elements using oval or rectangular
         * shapes. For more information, see [LarsonScanner.drawOval].
         * @param useGradients Whether to draw the eye using gradients. For more
         * information, see [LarsonScanner.useGradient].
         * @param useDivider Whether to use a divider to in-/decrement across the width
         * of the Larson Scanner. For more information, see [LarsonScanner.useDivider].
         * @param cylonAnimation Whether to animate the Larson Scanner as a Cylon-eye or
         * Kitt-eye. For more information, see [LarsonScanner.useCylonAnimation].
         * @param eyeColours One colour for each element in the eye. For
         * more information, see [eyeColours].
         * @param scannerBackgroundColour The background colour of the Larson Scanner.
         * @param eyeBackgroundColour The background colour of the eye in the Larson
         * Scanner.
         * @author Griefed
         */
        constructor(
            qualitySetting: Int,
            alphaValues: ShortArray,
            intervalInMillis: Short,
            divider: Short,
            numberOfElements: Byte,
            fractions: FloatArray,
            gapPercent: Float,
            partitionDivider: Double,
            forceAspectRatio: Boolean,
            ovalShaped: Boolean,
            useGradients: Boolean,
            useDivider: Boolean,
            cylonAnimation: Boolean,
            eyeColours: Array<Color>,
            scannerBackgroundColour: Color,
            eyeBackgroundColour: Color
        ) {
            this.qualitySetting = qualitySetting
            this.numberOfElements = numberOfElements
            alphas = alphaValues
            this.eyeColours = eyeColours
            interval = intervalInMillis
            this.divider = divider
            this.gapPercent = gapPercent
            this.partitionDivider = partitionDivider
            setFractions(fractions[0], fractions[1])
            isAspectRatioForced = forceAspectRatio
            isShapeOval = ovalShaped
            isGradientActive = useGradients
            isDividerActive = useDivider
            this.scannerBackgroundColour = scannerBackgroundColour
            this.eyeBackgroundColour = eyeBackgroundColour
            isCylonAnimation = cylonAnimation
        }

        /**
         * Set this configurations' fractions. For more information, see [LarsonScanner.setFractions].
         *
         * @param fractionOne First fraction for colour distribution for this
         * configuration.
         * @param fractionTwo Second fraction for colour distribution for this
         * configuration.
         * @author Griefed
         */
        @Throws(IllegalArgumentException::class)
        fun setFractions(fractionOne: Float, fractionTwo: Float) {
            when {
                fractionOne < 0.0f -> {
                    throw IllegalArgumentException(
                        "First fraction must not be negative. Specified $fractionOne"
                    )

                }

                fractionOne >= 1.0f -> {
                    throw IllegalArgumentException(
                        "First fraction must be smaller than 1.0f. Specified $fractionOne"
                    )

                }

                fractionOne > fractionTwo -> {
                    throw IllegalArgumentException(
                        "First fraction must be smaller than fraction two. Specified $fractionOne"
                    )

                }

                fractionTwo > 1.0f -> {
                    throw IllegalArgumentException(
                        "Second fraction must be bigger than the first, and smaller or equal to 1.0f. Specified $fractionTwo"
                    )

                }

                else -> {
                    this.fractions[0] = fractionOne
                    this.fractions[1] = fractionTwo
                }
            }

        }

        /**
         * Set whether this configurations' aspect ratio enforcing should be enabled. For more
         * information, see [LarsonScanner.forceAspectRatio].
         *
         * @param forceAspectRatio Whether this configuration should enforce aspect
         * ratio.
         * @author Griefed
         */
        fun setForceAspectRatio(forceAspectRatio: Boolean) {
            isAspectRatioForced = forceAspectRatio
        }

        /**
         * Set whether the elements for this configuration should be drawn as ovals or rectangles. For
         * more information, see [LarsonScanner.drawOval].
         *
         * @param ovalShaped Whether the elements for this configuration should be drawn as ovals.
         * @author Griefed
         */
        fun setOvalShaped(ovalShaped: Boolean) {
            isShapeOval = ovalShaped
        }

        /**
         * Set whether the elements for this configuration should be drawn using gradients. For more
         * information, see [LarsonScanner.useGradient].
         *
         * @param useGradients Whether the elements for this configuration should be
         * drawn using gradients.
         * @author Griefed
         */
        fun setUseGradients(useGradients: Boolean) {
            isGradientActive = useGradients
        }

        /**
         * Set whether the position for this configuration in-/decrements using a divider. For more
         * information, see [LarsonScanner.useDivider].
         *
         * @param useDivider Whether the position for this configuration should be
         * in-/decrement using a divider.
         * @author Griefed
         */
        fun setUseDivider(useDivider: Boolean) {
            isDividerActive = useDivider
        }

        companion object {
            /** Set the rendering quality of the Larson Scanner to low settings.  */
            const val LOW = 0

            /** Set the rendering quality of the Larson Scanner to medium settings.  */
            const val MEDIUM = 1

            /** Set the rendering quality of the Larson Scanner to high settings.  */
            const val HIGH = 2
        }
    }

    /**
     * The heart and soul of the Larson Scanner, the eye. This is the element which is being drawn and
     * animated within the panel of the LarsonScanner itself.
     *
     * @author Griefed
     */
    private inner class Eye : JComponent /*, Runnable */ {
        private val renderingHints = RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        val fractions = floatArrayOf(0.4f, 1.0f)

        /**
         * Get the current status of the animation, whether it is paused or not.
         *
         * @return Status of the animation. `false` if it is paused, true otherwise.
         * @author Griefed
         */
        @get:Synchronized
        @Volatile
        var isRunning = true
        var eyeColours =
            arrayOf(DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR, DEFAULT_EYE_COLOUR)
            set(value) {
                require(value.size == eye.numberOfElements.toInt()) {
                    "Color-array must contain exactly ${eye.numberOfElements} entries. Specified ${value.size}"
                }
                field = value
            }
        var gapPercent = 25.0f
        private var width = 0.0
        private var height = 0.0
        private var elementWidth = 0.0
        private var partition = 0.0
        private var gapWidth = 0.0
        private var totalGapWidth = 0.0
        private var halfOfTotalGapWidth = 0.0
        var partitionDivider = 5.0
        var lastSetRenderingQuality = 0
        var alphas = shortArrayOf(100, 200, 255, 200, 100)
        private var previousTime: Long = System.currentTimeMillis()
        private var position: Double = System.currentTimeMillis().toDouble()
        var interval: Short = 100
        var divider: Short = 250
        var numberOfElements: Byte = 5
        private var increasePosition = true
        var forceAspectRatio = false
        var ovalShaped = true
        var useGradients = true
        var useDivider = true
        var cylonAnimation = true

        /**
         * Default constructor for our eye, setting the background colour to black, the rendering
         * quality to low and the interval of the timer to a default value of 100ms.
         *
         * @author Griefed
         */
        constructor() : super() {
            isDoubleBuffered = true
            background = DEFAULT_BACKGROUND_COLOUR
            setRenderingQualityLow()
        }

        /**
         * Convenience constructor for an eye. This allows you to set the interval in ms at which the
         * eye is being drawn.
         *
         * @param updateInterval Interval in milliseconds at which to scroll.
         * @author Griefed
         */
        constructor(updateInterval: Short) : super() {
            this.interval = updateInterval
            isDoubleBuffered = true
            background = DEFAULT_BACKGROUND_COLOUR
            setRenderingQualityLow()
        }

        /**
         * Convenience constructor for an eye. This allows you to set the interval in ms at which the
         * eye is being drawn as well as the background of the eye.
         *
         * @param updateInterval Interval in milliseconds at which to scroll.
         * @param backgroundColor The background colour for the scanner and the eye.
         * @author Griefed
         */
        constructor(updateInterval: Short, backgroundColor: Color) : super() {
            this.interval = updateInterval
            isDoubleBuffered = true
            background = backgroundColor
            setRenderingQualityLow()
        }

        /**
         * Convenience constructor for an eye. This allows you to set the interval in ms at which the
         * eye is being drawn, the background of the eye, as well as the colour of the eye.
         *
         * @param updateInterval Interval in milliseconds at which to scroll.
         * @param backgroundColor The background colour for the scanner and the eye.
         * @param eyeColor The color of the eye.
         * @author Griefed
         */
        constructor(updateInterval: Short, backgroundColor: Color, eyeColor: Color) : super() {
            this.interval = updateInterval
            isDoubleBuffered = true
            setEyeColour(eyeColor)
            background = backgroundColor
            setRenderingQualityLow()
        }

        private lateinit var timer: Timer

        /**
         * Animate the eye! This method gets called after the thread is created in the constructor of
         * the parent [LarsonScanner] and started from there.
         *
         * By setting `paused` to either true or false you can pause or unpause the
         * animation respectively.
         *
         * If the animation is not paused, the position of the eye gets updated by calling [updatePosition] and then the eye gets drawn.
         *
         * @author Griefed
         */
        fun run() {
            val delay = 60
            timer = Timer(delay) {
                if (this.isRunning) {
                    eye.updatePosition()
                    eye.repaint()
                }
            }
            timer.delay = delay
            timer.isRepeats = true
            timer.isCoalesce = true
            timer.start()
        }

        /**
         * Pause the animation of the eye.
         *
         * @author Griefed
         */
        @Synchronized
        fun pauseAnimation() {
            this.isRunning = false
        }

        /**
         * Continue the animation of the eye.
         *
         * @author Griefed
         */
        @Synchronized
        fun playAnimation() {
            this.isRunning = true
        }

        /**
         * Toggle the current state of the animation.
         *
         *
         * If the animation is paused, then invoking this will unpause it.
         *
         *
         * If the animation is not paused, then invoking this will pause it.
         *
         * @author Griefed
         */
        @Synchronized
        fun togglePauseUnpause() {
            this.isRunning = !this.isRunning
        }

        /**
         * Draw the eye! Depending on whether oval shape is selected or gradients are to be used, the
         * eye is drawn in different ways. However, all draw calls come from here. I guess you could say
         * that this is the heart and soul of the eye itself. The eyeball, mayhaps? For details on how
         * each animation behaves, see [LarsonScanner.useCylonAnimation].
         *
         * @param g the `Graphics` object to protect
         * @author Griefed
         */
        override fun paint(g: Graphics) {
            super.paint(g)
            updateValues()
            val g2d = g as Graphics2D
            val fillHeight = height.roundToInt() + 10
            g2d.setRenderingHints(renderingHints)
            g2d.color = this.background
            g2d.fillRect(0, 0, width.roundToInt(), fillHeight)
            if (ovalShaped) {
                if (cylonAnimation) {
                    drawCylonOval(g2d)
                } else {
                    drawKittOval(g2d)
                }
            } else {
                if (cylonAnimation) {
                    drawCylonRect(g2d)
                } else {
                    drawKittRect(g2d)
                }
            }
            g2d.drawRect(0, -10, width.roundToInt(), fillHeight)
            g2d.dispose()
        }

        /**
         * Set the rendering quality with which the eye is being drawn to high.
         *
         * @author Griefed
         */
        fun setRenderingQualityHigh() {
            lastSetRenderingQuality = 2
            renderingHints[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
            renderingHints[RenderingHints.KEY_ALPHA_INTERPOLATION] = RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            renderingHints[RenderingHints.KEY_COLOR_RENDERING] = RenderingHints.VALUE_COLOR_RENDER_QUALITY
            renderingHints[RenderingHints.KEY_INTERPOLATION] = RenderingHints.VALUE_INTERPOLATION_BICUBIC
            renderingHints[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_QUALITY
        }

        /**
         * Set the rendering quality with which the eye is being drawn to medium.
         *
         * @author Griefed
         */
        fun setRenderingQualityMedium() {
            lastSetRenderingQuality = 1
            renderingHints[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_DEFAULT
            renderingHints[RenderingHints.KEY_ALPHA_INTERPOLATION] = RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT
            renderingHints[RenderingHints.KEY_COLOR_RENDERING] = RenderingHints.VALUE_COLOR_RENDER_DEFAULT
            renderingHints[RenderingHints.KEY_INTERPOLATION] = RenderingHints.VALUE_INTERPOLATION_BILINEAR
            renderingHints[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_DEFAULT
        }

        /**
         * Set the rendering quality with which the eye is being drawn to low. Default with which a new
         * eye is instantiated.
         *
         * @author Griefed
         */
        fun setRenderingQualityLow() {
            lastSetRenderingQuality = 0
            renderingHints[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_OFF
            renderingHints[RenderingHints.KEY_ALPHA_INTERPOLATION] = RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
            renderingHints[RenderingHints.KEY_COLOR_RENDERING] = RenderingHints.VALUE_COLOR_RENDER_SPEED
            renderingHints[RenderingHints.KEY_INTERPOLATION] = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            renderingHints[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_SPEED
        }

        /**
         * Set the same colour for every element in the eye.
         *
         * @param color The color to set for each element in the eye.
         * @author Griefed
         */
        fun setEyeColour(color: Color) {
            val newColours = arrayOfNulls<Color>(numberOfElements.toInt())
            for (i in 0 until numberOfElements) {
                newColours[i] = color
            }
            eyeColours = newColours.filterNotNull().toTypedArray()
        }

        /**
         * Update the width, height, partitioning and element width.
         *
         *
         * If the aspect ratio is being enforced, then the height of the eye are set to the width of
         * one element, resulting in a 1:1 aspect ratio.<br></br>
         * Otherwise, the width and height of the eye are set to the width and height of the
         * encompassing Larson Scanner.
         *
         *
         * If a change in width or height was detected, the size of the eye, the partitioning and the
         * element width are updated.
         *
         * @author Griefed
         */
        private fun updateValues() {
            var updated = false
            if (forceAspectRatio) {
                if (width != this@LarsonScanner.width.toDouble() || height != partition / numberOfElements.toDouble()) {
                    width = this@LarsonScanner.width.toDouble()
                    height = partition / numberOfElements.toDouble()
                    updated = true
                }
            } else {
                if (width != this@LarsonScanner.width.toDouble()) {
                    width = this@LarsonScanner.width.toDouble()
                    updated = true
                }
                if (height != this@LarsonScanner.height.toDouble()) {
                    height = this@LarsonScanner.height.toDouble()
                    updated = true
                }
            }
            if (updated) {
                setNewEyeValues()
            }
        }

        /**
         * Set the new values for the eyes
         *  * Size with width and height
         *  * partitioning of the eye
         *  * width of a single element in the eye
         *  * width of the pag between two elements when drawing as rectangles
         *  * total width of all gaps when drawing as rectangles
         *  * half of the total width of all gaps when drawing as rectangles
         *
         * @author Griefed
         */
        fun setNewEyeValues() {
            // The eye itself is to be a fifth of the whole width
            partition = width / partitionDivider
            elementWidth = partition / numberOfElements.toDouble()
            gapWidth = elementWidth / 100.0 * gapPercent
            totalGapWidth = gapWidth * (numberOfElements - 2)
            halfOfTotalGapWidth = totalGapWidth / 2.0
        }

        /**
         * Draw our elements in oval shape. If `useGradient` is set, then gradients are used
         * for painting, otherwise our ovals are painted with solid colours. For details on how this
         * animation behaves, see [LarsonScanner.useCylonAnimation].
         *
         * @param g2d To fill and draw with.
         * @author Griefed
         */
        private fun drawCylonOval(g2d: Graphics2D) {
            for (element in 0 until numberOfElements) {
                val startOfElement = calcCylonOvalStart(element.toByte())
                if (useGradients) {
                    val center = getCenter(startOfElement)
                    val color = eyeColours[element]
                    g2d.paint = cylonRadialGradient(element.toByte(), center, color)
                } else {
                    g2d.color = eyeColours[element]
                }
                g2d.fillOval(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
            }
        }

        /**
         * Calculate the Y-coordinate of the center of the element currently being drawn.
         *
         *
         * Divide the width of an element by two and add that to the play of the current element
         * being drawn.
         *
         * @param start The play point of the element currently being drawn.
         * @return The Y-coordinate of the center of the element currently being drawn.
         * @author Griefed
         */
        private fun getCenter(start: Double): Double {
            return start + elementWidth / 2.0
        }

        /**
         * Calculate the position of an element in oval shape. Steps are:
         *
         *  * Multiply the width of one element with the number of elements in the eye. Divide by two
         *  * multiply the width of one element with the number of the element currently being drawn
         * (0 to total number of elements in the eye)
         *  * add the values from above together
         *  * subtract the above value from the current position in the eye being drawn at
         *
         * The element width multiplied with the number of elements, then divided by two, gives the half of the total
         * width of the eye itself.
         * Added to that the width of one element, multiplied with the number of the current element being drawn in the
         * eye, and we get the position of the eye we are currently drawing.
         * To put this into relation along the width of the whole LarsonScanner, we need to subtract that value from the
         * current position along the Larson Scanners total width.
         *
         * @param element The number of the currently being drawn element, ranging from 0
         * to the amount of elements in the eye.
         * @return The X-coordinate where the current element starts.
         * @author Griefed
         */
        @Suppress("SameParameterValue", "SameParameterValue")
        private fun calcCylonOvalStart(element: Byte): Double {
            val singleWidth = elementWidth * element.toDouble()
            val totalWidth = elementWidth * numberOfElements.toDouble()
            val halfOfEye = totalWidth / 2.0
            val currentlyDrawing = halfOfEye + singleWidth
            return position - currentlyDrawing
        }

        /**
         * Create the radial gradient for the currently being drawn, oval-shaped, element. Radial
         * gradients extend from the 2D center point of the current element to the height of one
         * element.
         *
         * @param element The number of the element currently being drawn.
         * @param centerX The X-coordinate of the center of the element currently being
         * drawn.
         * @param color The color of the element currently being drawn.
         * @return A radial gradient for the currently being drawn,
         * oval-shaped, element.
         * @author Griefed
         */
        private fun cylonRadialGradient(
            element: Byte, centerX: Double, color: Color
        ): RadialGradientPaint {
            val alphaColour = colourWithAlpha(alphas[element.toInt()], color)
            val colors = arrayOf(alphaColour, background)
            val radius = Point(centerX.roundToInt(), (height / 2).roundToInt())
            val fraction = (0.5f * height).toFloat()
            return RadialGradientPaint(radius, fraction, fractions, colors)
        }

        /**
         * Draw our element in rectangular shape. If `useGradient` is set, then gradients are
         * used for painting, otherwise our rectangles are painted with solid colours. For details on
         * how this animation behaves, see [LarsonScanner.useCylonAnimation].
         *
         * @param g2d Fill and draw with.
         * @author Griefed
         */
        private fun drawCylonRect(g2d: Graphics2D) {
            for (element in 0 until numberOfElements) {
                val startOfElement = calcCylonRectStart(element.toByte())
                if (useGradients) {
                    val median = (numberOfElements + 1) / 2
                    if (element + 1 < median) {
                        g2d.paint = ascCylonRectGradient(element.toByte(), startOfElement)
                    } else if (element + 1 == median) {
                        g2d.paint = eyeColours[element]
                    } else {
                        g2d.paint = descCylonRectGradient(element.toByte(), startOfElement)
                    }
                } else {
                    g2d.color = eyeColours[element]
                }
                g2d.fillRect(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
            }
        }

        /**
         * Calculate the play of an element in rectangular shape. Steps are:
         *
         *
         *  * **Calculate the width of a gap:**
         *
         *  * Divide the width of one element by 100
         *  * multiply with the currently set gap-percentage. (Default `25.0f`)
         *
         *  * **Calculate the half of the total gap width, across all elements:**
         *
         *  * Subtract two from the number of elements in the eye. No gaps before the first, or
         * after the last element, we only care about the ones between.
         *  * multiply with the width of one gap
         *  * divide by two
         *
         *  * **When the current element is the first of all elements in the eye:**
         *
         *  * Multiply the width of an element with the number of elements in the eye. Divide
         * by two
         *  * subtract the previously calculated half of the total gap width, across all
         * elements
         *  * subtract that from the current position in the eye being drawn at
         *
         *  * **When the current element is not the first of all elements in the eye:**
         *
         *  * Multiply the width of an element with the number of elements in the eye. Divide
         * by two
         *  * subtract the previously calculated half of the total gap width, across all
         * elements
         *  * add the width of one element to the gap width, multiply with the number of the
         * element currently being drawn
         *  * Subtract that from the current position in the eye being drawn at
         *
         *
         *
         * @param element The number of the element currently being drawn.
         * @return The X-coordinate where the current element starts.
         * @author Griefed
         */
        private fun calcCylonRectStart(element: Byte): Double {
            return if (element.toInt() == 0) {
                val eyeWidth = elementWidth * numberOfElements
                val halfOfEye = eyeWidth / 2.0
                val start = position - halfOfEye - halfOfTotalGapWidth
                start
            } else {
                val eyeWidth = elementWidth * numberOfElements
                val halfOfEye = eyeWidth / 2.0
                val newPosition = position - halfOfEye - halfOfTotalGapWidth
                val elementStart = (elementWidth + gapWidth) * element
                val start = newPosition + elementStart
                start
            }
        }

        /**
         * Create the gradient for the currently being drawn, rectangular-shaped, element, when the
         * element is to the left of the center element and when animating as a Cylon-eye. Elements to
         * the left of the center element must have gradients which increase in color intensity towards
         * the center of the eye.
         *
         * Every gradient for these rectangular shapes is drawn from:
         *
         *  * `X-coordinate:` Acquired from [calcCylonRectStart]
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * to:
         *
         *  * `X-coordinate:` Acquired from [calcCylonRectStart] plus the
         * width of one element.
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * @param element The number of the current element being drawn.
         * @param startX The X-coordinate where the current element starts.
         * @return The gradient with which to draw the current element.
         * @author Griefed
         */
        private fun ascCylonRectGradient(element: Byte, startX: Double): GradientPaint {
            val elementAlpha = alphas[element.toInt()]
            val currentColour = eyeColours[element.toInt()]
            val y1 = (height / 2.0).toFloat()
            val alpha1 = (elementAlpha / 2).toShort()
            val colour1 = colourWithAlpha(alpha1, currentColour)
            val x2 = (startX + elementWidth).toFloat()
            val y2 = (height / 2.0).toFloat()
            val colour2 = colourWithAlpha(elementAlpha, currentColour)
            return GradientPaint(startX.toFloat(), y1, colour1, x2, y2, colour2)
        }

        /**
         * Create the gradient for the currently being drawn, rectangular-shaped, element, when the
         * element is to the right of the center element and when animating as a Cylon-eye. Elements to
         * the right of the center element must have gradients which decrease in color intensity towards
         * the end of the eye.
         *
         * Every gradient for these rectangular shapes is drawn from:
         *
         *  * `X-coordinate:` Acquired from [calcCylonRectStart]
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * to:
         *
         *  * `X-coordinate:` Acquired from [calcCylonRectStart] plus the
         * width of one element.
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * @param element The number of the current element being drawn.
         * @param startX The X-coordinate where the current element starts.
         * @return The gradient with which to draw the current element.
         * @author Griefed
         */
        private fun descCylonRectGradient(element: Byte, startX: Double): GradientPaint {
            val elementAlpha = alphas[element.toInt()]
            val currentColour = eyeColours[element.toInt()]
            val y1 = (height / 2.0).toFloat()
            val colour1 = colourWithAlpha(elementAlpha, currentColour)
            val x2 = (startX + elementWidth).toFloat()
            val y2 = (height / 2.0).toFloat()
            val alpha1 = (elementAlpha / 2).toShort()
            val colour2 = colourWithAlpha(alpha1, currentColour)
            return GradientPaint(startX.toFloat(), y1, colour1, x2, y2, colour2)
        }

        /**
         * Draw our elements in oval shape, starting from the current position to the current position
         * plus the width of the eye. If `useGradient` is set, then gradients are used for
         * painting, otherwise our ovals are painted with solid colours. For details on how this
         * animation behaves, see [LarsonScanner.useCylonAnimation].
         *
         * @param g2d Fill and draw with.
         * @author Griefed
         */
        private fun drawKittOval(g2d: Graphics2D) {
            var startOfElement: Double
            val posDrawn: Double
            var elementToDraw: Byte
            val elementCenter: Double
            val elementColour: Color
            for (element in 0 until numberOfElements) {
                startOfElement = calcKittOvalStart(element.toByte())
                addKittOval(g2d, startOfElement, element.toByte())
            }
            if (increasePosition) {
                // Going left to right
                posDrawn = position + (numberOfElements * elementWidth)
                if (posDrawn >= width) {
                    /*
                     * We are entering the nether on the right side, so we draw the brightest element at the
                     * most right position to create the illusion of the elements gathering.
                     */
                    startOfElement = width - elementWidth
                    elementToDraw = (numberOfElements - 1).toByte()
                    elementCenter = getCenter(startOfElement)
                    elementColour = eyeColours[numberOfElements - 1]
                    if (useGradients) {
                        g2d.paint = kittRadialGradient(elementToDraw, elementCenter, elementColour)
                    } else {
                        g2d.color = elementColour
                    }
                    g2d.fillOval(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
                } else if (position < 0) {
                    /*
                     * We are leaving the nether on the left side, so we need to draw that the next element
                     * after the ones already visible to create the illusion of the eye emerging.
                     */
                    val part = posDrawn / elementWidth
                    val preElement = numberOfElements - floor(part)
                    elementToDraw = (preElement - 1).roundToInt().toByte()
                    elementColour = eyeColours[elementToDraw.toInt()]
                    if (useGradients) {
                        elementCenter = getCenter(0.0)
                        g2d.paint = kittRadialGradient(elementToDraw, elementCenter, elementColour)
                    } else {
                        g2d.color = elementColour
                    }
                    g2d.fillOval(0, 0, elementWidth.roundToInt(), height.roundToInt())
                }
            } else {
                // Going right to left
                posDrawn = position - numberOfElements * elementWidth
                if (posDrawn <= 0) {
                    /*
                     * We are entering the nether on the left side, so we draw the brightest element at the
                     * most left position to create the illusion of the elements gathering.
                     */
                    elementColour = eyeColours[numberOfElements - 1]
                    if (useGradients) {
                        elementToDraw = (numberOfElements - 1).toByte()
                        elementCenter = getCenter(0.0)
                        g2d.paint = kittRadialGradient(elementToDraw, elementCenter, elementColour)
                    } else {
                        g2d.color = elementColour
                    }
                    g2d.fillOval(0, 0, elementWidth.roundToInt(), height.roundToInt())
                } else if (position >= width) {
                    /*
                     * We are leaving the nether on the right side, so we need to draw the next element
                     * after the ones already visible to create the illusion of the eye emerging.
                     */
                    startOfElement = width - elementWidth
                    elementToDraw = ((position - width) / elementWidth).roundToInt().toByte()
                    if (elementToDraw >= numberOfElements) {
                        elementToDraw = (numberOfElements - 1).toByte()
                    }
                    if (elementToDraw < numberOfElements) {
                        addKittOval(g2d, startOfElement, elementToDraw)
                    }
                }
            }
        }

        /**
         * Helper method to slightly cleanup [drawKittOval].
         *
         * @param g2d Fill and draw with.
         * @param startOfElement The play of the current element along the Larson
         * Scanner.
         * @param element The element we are currently drawing.
         * @author Griefed
         */
        private fun addKittOval(g2d: Graphics2D, startOfElement: Double, element: Byte) {
            val elementColour = eyeColours[element.toInt()]
            if (useGradients) {
                val elementCenter = getCenter(startOfElement)
                g2d.paint = kittRadialGradient(element, elementCenter, elementColour)
            } else {
                g2d.color = elementColour
            }
            g2d.fillOval(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
        }

        /**
         * Calculate the play of an element in oval shape when animating in Kitt-style and moving from
         * left to right, or right to left, depending on the direction.
         *
         *
         * **left to right:**
         *
         *
         *  * Multiply the element currently being drawn with the width of one element
         *  * add the previous to the current position from which we are drawing
         *
         *
         * The element width multiplied with the element currently being drawn, then added on top of the
         * current position in the Larson Scanner, results in the starting point along the X-axis from
         * which to draw the current element.
         *
         *
         * **right to left**
         *
         *
         *  * Multiply the element currently being drawn, plus 1, with the width of one element
         *  * subtract the previous from the current position from which we are drawing
         *
         *
         * The element width multiplied with the element currently being drawn, plus 1 because we want
         * the element right from the leftmost one, then subtracted from the current position in the
         * Larson Scanner, results in the starting point along the X-axis from which to draw the current
         * element.
         *
         * @param element The number of the currently being drawn element, ranging from 0
         * to the amount of elements in the eye.
         * @return The X-coordinate where the current element starts.
         * @author Griefed
         */
        private fun calcKittOvalStart(element: Byte): Double {
            return if (increasePosition) {
                position + element * elementWidth
            } else {
                position - (element + 1) * elementWidth
            }
        }

        /**
         * Create the radial gradient for the currently being drawn, oval-shaped, element, when
         * animating Kitt-style. Radial gradients extend from the 2D center point of the current element
         * to the height of one element. As the alpha decreases in the Kitt-style animation, we work
         * with percentages of 255, where an alpha value of 255 equals 0 transparency.
         *
         * @param element The number of the element currently being drawn.
         * @param centerX The X-coordinate of the center of the element currently being
         * drawn.
         * @param color The color of the element currently being drawn.
         * @return A radial gradient for the currently being drawn,
         * oval-shaped, element.
         * @author Griefed
         */
        private fun kittRadialGradient(element: Byte, centerX: Double, color: Color): RadialGradientPaint {
            val alphaFraction = 255.0 / numberOfElements
            val alpha = (alphaFraction * (element + 1)).roundToInt().toShort()
            val colour = colourWithAlpha(alpha, color)
            val colors = arrayOf(colour, background)
            val point = Point(centerX.roundToInt(), (height / 2).roundToInt())
            val radius = (0.5f * height).toFloat()
            return RadialGradientPaint(point, radius, fractions, colors)
        }

        /**
         * Draw our element in rectangular shape. If `useGradient` is set, then gradients are
         * used for painting, otherwise our rectangles are painted with solid colours. For details on
         * how this animation behaves, see [LarsonScanner.useCylonAnimation].
         *
         * @param g2d Fill and draw with.
         * @author Griefed
         */
        @Suppress("SameParameterValue")
        private fun drawKittRect(g2d: Graphics2D) {
            var startOfElement = 0.0
            val posDrawn: Double
            var elementToDraw: Byte
            val elementCenter: Double
            for (element in 0 until numberOfElements) {
                startOfElement = calcKittRectStart(element.toByte())
                if (useGradients) {
                    g2d.paint = kittRectGradient(element.toByte(), startOfElement)
                } else {
                    g2d.color = eyeColours[element]
                }
                g2d.fillRect(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
            }
            if (increasePosition) {
                // Going left to right
                val totalGapWidth = (numberOfElements - 1) * gapWidth
                val totalElementWidth = numberOfElements * elementWidth
                posDrawn = position + totalElementWidth + totalGapWidth
                if (posDrawn >= width) {
                    /*
                     * We are entering the nether on the right side, so we draw the brightest element at the
                     * most right position to create the illusion of the elements gathering.
                     */
                    startOfElement = width - elementWidth
                    g2d.color = eyeColours[numberOfElements - 1]
                    g2d.fillRect(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
                } else if (position <= 0) {
                    /*
                     * We are leaving the nether on the left side, so we need to draw that the next element
                     * after the ones already visible to create the illusion of the eye emerging.
                     */
                    elementToDraw = (numberOfElements - posDrawn / elementWidth - 1).roundToInt().toByte()
                    if (elementToDraw in 0 until numberOfElements) {
                        if (useGradients) {
                            elementCenter = getCenter(0.0)
                            g2d.paint = kittRectGradient(elementToDraw, elementCenter)
                        } else {
                            g2d.color = eyeColours[elementToDraw.toInt()]
                        }
                        g2d.fillRect(0, 0, elementWidth.roundToInt(), height.roundToInt())
                    }
                }
            } else {
                // Going right to left
                if (startOfElement <= 0) {
                    /*
                     * We are entering the nether on the left side, so we draw the brightest element at the
                     * most left position to create the illusion of the elements gathering.
                     */
                    if (useGradients) {
                        elementCenter = getCenter(0.0)
                        g2d.paint = kittRectGradient((numberOfElements - 1).toByte(), elementCenter)
                    } else {
                        g2d.color = eyeColours[numberOfElements - 1]
                    }
                    g2d.fillRect(0, 0, elementWidth.roundToInt(), height.roundToInt())
                } else if (position >= width) {
                    /*
                     * We are leaving the nether on the right side, so we need to draw that the next element
                     * after the ones already visible to create the illusion of the eye emerging.
                     */
                    startOfElement = width - elementWidth
                    elementToDraw = ((position - width) / elementWidth).roundToInt().toByte()
                    if (elementToDraw >= numberOfElements) {
                        elementToDraw = (numberOfElements - 1).toByte()
                    }
                    if (elementToDraw >= 0) {
                        if (useGradients) {
                            elementCenter = getCenter(startOfElement)
                            g2d.paint = kittRectGradient(elementToDraw, elementCenter)
                        } else {
                            g2d.color = eyeColours[elementToDraw.toInt()]
                        }
                    }
                    g2d.fillRect(startOfElement.roundToInt(), 0, elementWidth.roundToInt(), height.roundToInt())
                }
            }
        }

        /**
         * Calculate the play of a Kitt-style animated rectangle. When the element we are drawing is the
         * very first element, the play of said rectangle is simply the current position along the
         * X-axis. Otherwise, when we are scrolling
         *
         * **left to right**
         *
         *  * Add the width of an element with the width of the gap between two elements
         *  * multiply with the number of the current element
         *  * add the result to the current position along the X-axis.
         *
         * **right to left**
         *
         *  * Add the width of an element with the width of the gap between two elements
         *  * multiply with the number of the current element
         *  * subtract the result from the current position along the X-axis.
         *
         * @param element The number of the element currently being drawn.
         * @return The X-coordinate where the current element starts.
         * @author Griefed
         */
        private fun calcKittRectStart(element: Byte): Double {
            if (element.toInt() == 0) {
                return position
            }
            return if (increasePosition) {
                position + element * (elementWidth + gapWidth)
            } else {
                position - element * (elementWidth + gapWidth)
            }
        }

        /**
         * Create the gradient for the currently being drawn, rectangular-shaped, element, when
         * animating as a Kitt-eye. Elements have gradients with increased alpha, so less transparency,
         * the further they go from left to right, or the other way around when going right to left.
         *
         * Every gradient for these rectangular shapes is drawn from:
         *
         *  * `X-coordinate:` Acquired from [calcKittRectStart]
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * to:
         *
         *  * `X-coordinate:` Acquired from [calcKittRectStart] plus the
         * width of one element.
         *  * `Y-coordinate:` Half of the height of one element.
         *
         * @param element The number of the current element being drawn.
         * @param startX The X-coordinate where the current element starts.
         * @return The gradient with which to draw the current element.
         * @author Griefed
         */
        private fun kittRectGradient(element: Byte, startX: Double): GradientPaint {
            val alphaOne = (255.0 / numberOfElements * (element + 1).toDouble()).roundToInt().toShort()
            val alphaTwo = (255.0 / numberOfElements * (element + 1).toDouble()).roundToInt().toShort()
            val x1 = startX.toFloat()
            val y1 = (height / 2.0).toFloat()
            val x2 = (startX + elementWidth).toFloat()
            val y2 = (height / 2.0).toFloat()
            val eyeColour = eyeColours[element.toInt()]
            val colour1: Color
            val colour2: Color
            return if (increasePosition) {
                colour1 = colourWithAlpha(alphaOne, eyeColour)
                colour2 = colourWithAlpha(alphaTwo, eyeColour)
                GradientPaint(x1, y1, colour1, x2, y2, colour2)
            } else {
                colour1 = colourWithAlpha(alphaTwo, eyeColour)
                colour2 = colourWithAlpha(alphaOne, eyeColour)
                GradientPaint(x1, y1, colour1, x2, y2, colour2)
            }
        }

        /**
         * Set the alpha value for the current colour of the current element being drawn. The alpha
         * value must be a number in the range of `0` to `255`.
         *
         * @param alpha Alpha value to set with the given colour. Ranging from `0` to `255`.
         * @param color Color of the element currently being drawn.
         * @return The color of the element currently being drawn with an alpha value set.
         * @throws IllegalArgumentException if the specified alpha-value is smaller than `0`
         * or greater than `255`.
         * @author Griefed
         */
        @Throws(IllegalArgumentException::class)
        private fun colourWithAlpha(alpha: Short, color: Color): Color {
            require(!(alpha < 0 || alpha > 255)) { "Alpha must be 0 to 255. Specified $alpha" }
            return Color(color.red, color.green, color.blue, alpha.toInt())
        }

        /**
         * Update the position at which we are currently drawing the eye.
         *
         *  * Cylon Style
         *
         *  * If `useDivider` is set, then the position is in-/decremented with the
         * division of the width of the Larson Scanner and the currently set divider.
         *  * If `useDivider` is not set, then the position is in-/decremented by 1.
         *  * When the window the Larson Scanner resides in is resized, and the position is out
         * of the visible area, then the position is forcefully set to either the width of
         * the Larson Scanner, or 0, depending on whether the position is outside the
         * visible field to the left or right.
         *
         *  * Kitt Style
         *
         *  * If `useDivider` is set, then the position is in-/decremented with the
         * division of the width of the Larson Scanner and the currently set divider.
         *  * If `useDivider` is not set, then the position is in-/decremented by 1.
         *  * When the window the Larson Scanner resides in is resized, and the position is out
         * of the visible area, then the position is forcefully set to either the width plus
         * the max width of the eye of the Larson Scanner, or 0 minus the max width of the
         * eye, depending on whether the position is outside the visible field to the left
         * or right.
         *  * In Kitt-style animation, the position does **not** scroll from 0 to
         * the width of the Larson Scanner and the other way around. Instead, the total
         * scroll width of the Larson Scanner is increased in both directions by the total
         * width of the eye
         *
         * @author Griefed
         */
        private fun updatePosition() {
            if (cylonAnimation) {
                updatePositionCylonStyle()
            } else {
                updatePositionKittStyle()
            }
        }

        /**
         * See [updatePosition] for details.
         *
         * @author Griefed
         */
        private fun updatePositionCylonStyle() {
            if (position < 0) {
                // switch to left to right
                increasePosition = true
                position = 0.0
                return
            } else if (position > width) {
                // switch to right to left
                increasePosition = false
                position = width
                return
            }
            val deltaTime = System.currentTimeMillis() - previousTime
            val positionChange = if (useDivider) {
                deltaTime * interval.toDouble() / divider.toDouble()
            } else {
                deltaTime * interval.toDouble() / 1000.0
            }
            if (increasePosition && position < width) {
                // left to right
                position += positionChange
            } else if (!increasePosition && position > 0) {
                // right to left
                position -= positionChange
            }
            previousTime = System.currentTimeMillis()
        }

        /**
         * See [updatePosition] for details.
         *
         * @author Griefed
         */
        private fun updatePositionKittStyle() {
            val widthElements: Double = if (ovalShaped) {
                numberOfElements * elementWidth
            } else {
                numberOfElements * elementWidth + totalGapWidth
            }
            val maxWidth = width + widthElements
            val maxNegative = 0 - widthElements
            if (position < maxNegative) {
                // switch to left to right
                increasePosition = true
                position = maxNegative
                return
            } else if (position > maxWidth) {
                // switch to right to left
                increasePosition = false
                position = maxWidth
                return
            }
            val deltaTime = System.currentTimeMillis() - previousTime
            val positionChange = if (useDivider) {
                deltaTime * interval.toDouble() / divider.toDouble()
            } else {
                deltaTime * interval.toDouble() / 1000.0
            }
            if (increasePosition && position < maxWidth) {
                // left to right
                position += positionChange
            } else if (!increasePosition && position > maxNegative) {
                // right to left
                position -= positionChange
            }
            previousTime = System.currentTimeMillis()
        }
    }

    companion object {
        val DEFAULT_BACKGROUND_COLOUR = Color(0, 0, 0)
        val DEFAULT_EYE_COLOUR = Color(255, 0, 0)
    }
}
