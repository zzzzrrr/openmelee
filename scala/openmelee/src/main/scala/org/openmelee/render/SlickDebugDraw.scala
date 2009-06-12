package org.openmelee.render

import org.villane.vecmath._
import org.villane.box2d.draw._

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.opengl.TextureImpl;

/**
 * Not fully implemented - just enough here to get the Pyramid
 * demo to draw.
 *
 */
class SlickDebugDraw(var g: Graphics, var container: GameContainer) extends DebugDrawHandler {
  // World 0,0 maps to transX, transY on screen
  var transX = 300.0f
  var transY = 300.0f
  var scaleFactor = 12.0f
  val yFlip = -1.0f

  def map(mapMe: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float) = {
    val interp = (mapMe - fromLow) / (fromHigh - fromLow)
    (interp*toHigh + (1.0f-interp)*toLow)
  }
    
  override def worldToScreen(world: Vector2f) = {
    val x = map(world.x, 0f, 1f, transX, transX+scaleFactor)
    var y = map(world.y, 0f, 1f, transY, transY+scaleFactor)
    if (yFlip == -1.0f) y = map(y, 0f, container.getHeight, container.getHeight, 0f)
    Vector2f(x, y)
  }

  override def screenToWorld(screen: Vector2f) = {
    val x = map(screen.x, transX, transX+scaleFactor, 0f, 1f)
    var y = screen.y
    if (yFlip == -1.0f) y = map(y, container.getHeight, 0f, 0f, container.getHeight)
    y = map(y, transY, transY + scaleFactor, 0f, 1f)
    Vector2f(x, y)
  }

  def drawCircle(center: Vector2f, radius: Float, color: Color3f) {
    val c = worldToScreen(center)
    val circle = new Circle(c.x, c.y, radius * scaleFactor)
    val slickColor = new Color(color.r/255.0f,color.g/255.0f,color.b/255.0f)
    g.setColor(slickColor)
    g.draw(circle)
  }

  def drawPoint(position: Vector2f, f: Float, color: Color3f) {}

  def drawPolygon(vertices: Array[Vector2f], color: Color3f) {
    val polygon = new Polygon()
    for (v <- vertices) {
      val screenPt = worldToScreen(v)
      polygon.addPoint(screenPt.x, screenPt.y)
    }
    val slickColor = new Color(color.r/255.0f,color.g/255.0f,color.b/255.0f)
    g.setColor(slickColor)
    g.draw(polygon)
  }

  def drawSolidPolygon(vertices: Array[Vector2f], color: Color3f) {
    val polygon = new Polygon()
    for (v <- vertices) {
      val screenPt = worldToScreen(v)
      polygon.addPoint(screenPt.x, screenPt.y)
    }
    val slickColor = new Color(color.r/255.0f,color.g/255.0f,color.b/255.0f)
    g.setColor(slickColor)
    g.draw(polygon)
	slickColor.a = 0.5f
    g.fill(polygon)
  }

  def drawSegment(p1: Vector2f, p2: Vector2f, color: Color3f) {
    val slickColor = new Color(color.r/255.0f,color.g/255.0f,color.b/255.0f)
    g.setColor(slickColor)
    TextureImpl.bindNone()
    g.setLineWidth(1)
    g.setAntiAlias(false)
    val screen1 = worldToScreen(p1)
    val screen2 = worldToScreen(p2)
    g.drawLine(screen1.x,screen1.y,screen2.x,screen2.y)
  }

  def drawSolidCircle(center: Vector2f, radius: Float, axis: Vector2f, color: Color3f) {
    val c = worldToScreen(center)
    val circle = new Circle(c.x, c.y, radius * scaleFactor)
    val slickColor = new Color(color.r/255.0f,color.g/255.0f,color.b/255.0f)
    g.setColor(slickColor)
    g.fill(circle)
  }

  def drawString(x: Float, y: Float, s: String, color: Color3f) {}

  def drawTransform(xf: Transform2f) {}

}