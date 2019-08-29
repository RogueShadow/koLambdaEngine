package net.granseal.koLambda

import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.system.exitProcess


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

fun keyHeld(keys: List<Int>): Boolean {
    for (k in keys) {
        if (keyHeld(k))return true
    }
    return false
}
fun keyPressed(keys: List<Int>): Boolean {
    for (k in keys) {
        if (keyPressed(k))return true
    }
    return false
}
fun keyHeld(code: Int) = Input.keyHeld(code)
fun keyHeld(char: Char) = Input.keyHeld(char)
fun keyPressed(code: Int) = Input.keyPressed(code)
fun keyPressed(char: Char) = Input.keyPressed(char)
fun mousePos(): Point2D.Float = Input.mouse.point()
fun mouseButton(b: Int) = Input.mouse.buttonsHeld[b]!!

fun getStream(file: String): InputStream {
    lateinit var stream: InputStream
    val path = "res/$file"
    try {
         stream = ::ApplicationAdapter.javaClass.classLoader.getResourceAsStream(path)!!
    }catch (e: Exception){
        println("Failed to get stream from $path")
        println("I was looking in ${File(path).absolutePath}")
        println("I was looking in ${File(path).path}")
        println("I was looking in ${File(path).canonicalPath}")
        e.printStackTrace()
        exitProcess(-1)
    }
    println("Loaded stream $path")
    return stream
}
fun getResource(file: String): URL = ::ApplicationAdapter.javaClass.getResource("res/$file")
fun getReader(file: String) = getStream(file).reader()
fun getBufReader(file: String) = getStream(file).bufferedReader()

fun lerp(values: List<Float>,percent: Float, loop: Boolean = false, e: (Float) -> Float = ::Linear): Float {
    var p = percent
    if (loop) p = percent % 1
    if (values.size == 1)return values.first()
    if (values.isEmpty())throw Exception("values must have at least one value.")

    val percent2 = p * values.size
    val index = percent2.toInt()
    val remainder = percent2 - index
    if (!loop) {
        if (index >= values.size - 1) return values.last()
        return interpolate(values[index], values[index + 1], remainder,e)
    }else{
        if (index >= values.size && !loop)return values.last()
        if (index >= values.size && loop)return values.first()
        val v1 = values[index]
        return if (index >= values.size -1){
            val v2 = values[0]
            interpolate(v1,v2,remainder,e)
        }else{
            val v2 = values[index+1]
            interpolate(v1,v2,remainder,e)
        }
    }
}

fun lerp(colors: List<Color>, percent: Float, loop: Boolean = false, e: (Float) -> Float = ::Linear): Color {
    val fRGB = colors.map { it.getColorComponents(null)}
    val r = fRGB.map { it[0] }
    val g = fRGB.map { it[1] }
    val b = fRGB.map { it[2] }
    val ir = lerp(r,percent,loop,e)
    val ig = lerp(g,percent,loop,e)
    val ib = lerp(b,percent,loop,e)
    return Color(clamp(ir,0f,1f),clamp(ig,0f,1f),clamp(ib,0f,1f))
}

fun lerp(points: List<Point2D.Float>, percent: Float, loop: Boolean = false, e: (Float) -> Float = ::Linear): Point2D.Float {
    val x = points.map{ it.x }
    val y = points.map{ it.y }
    val ix = lerp(x,percent,loop,e)
    val iy = lerp(y,percent,loop,e)
    return point(ix,iy)
}


inline fun interpolate(start: Float, end: Float, percent: Float, e: (Float) -> Float = ::Linear): Float {
    return start + (e(percent) * (end - start))
}

fun <T: Comparable<T>> clamp(value: T, min: T, max: T): T {
    if (value < min) return min
    if (value > max)return max
    return value
}

fun Linear(t: Float) = t
fun SmoothStart(t: Float) = t*t
fun SmoothStop(t: Float) = 1 - (1-t)*(1-t)
fun SmoothStart2(t: Float) = t*t*t
fun SmoothStop2(t: Float) = 1 - (1-t)*(1-t)*(1-t)

fun <T> List<List<T>>.get(p: Point2D): T {
    return this[p.y.toInt()][p.x.toInt()]
}