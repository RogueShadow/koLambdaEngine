package net.granseal.koLambda

import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.io.InputStream
import java.net.URL


fun point(x: Float, y: Float) = Point2D.Float(x,y)
fun point() = point(0f, 0f)
fun rect(x: Float = 0f,y: Float = 0f, w: Float = 0f, h: Float = 0f) = Rectangle2D.Float(x,y,w,h)
fun rColor() = Color(Math.random().toFloat(),Math.random().toFloat(),Math.random().toFloat())
fun hsb(hue: Float, sat: Float, bright: Float): Color {
    return Color.getHSBColor(hue,sat,bright)
}
fun Color.invert(): Color {
    val hsb = Color.RGBtoHSB(red,green,blue,null)
    hsb[0] += 0.5f
    if (hsb[0] > 1f)hsb[0] -= 1f
    return Color.getHSBColor(hsb[0],hsb[1],hsb[2])
}
fun keyDown(code: Int) = Input.keyHeld(code)
fun keyDown(char: Char) = Input.keyHeld(char)
fun mousePos(): Point2D.Float = Input.mouse.point()
fun mouseButton(b: Int) = Input.mouse.buttonsHeld[b]

fun getStream(file: String): InputStream = ::ApplicationAdapter.javaClass.getResourceAsStream("../res/$file")
fun getResource(file: String): URL = ::ApplicationAdapter.javaClass.getResource("../res/$file")
fun getReader(file: String) = getStream(file).reader()
fun getBufReader(file: String) = getStream(file).bufferedReader()


