package net.granseal.koLambda

import java.awt.Point
import java.awt.geom.Point2D.Float as F2

infix operator fun F2.plusAssign(other: F2) {
    this.setLocation(this + other)
}
infix operator fun F2.times(other: Float): F2 {
    return F2(this.x*other,this.y*other)
}
infix operator fun F2.plus(other: F2): F2 {
    return F2(this.x+other.x,this.y+other.y)
}
infix operator fun F2.minus(other: F2): F2 {
    return F2(this.x-other.x,this.y-other.y)
}
fun Point.toFloat(): F2 {
    return F2(x.toFloat(), y.toFloat())
}