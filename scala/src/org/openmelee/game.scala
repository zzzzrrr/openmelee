/*
 * Game.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.openmelee

import org.openmelee.melee.Melee
import org.openmelee.render.Render

class Game {

    def init() = {
        val melee = new Melee
        val frame = new javax.swing.JFrame("Test")
        val applet = melee.render
        frame.getContentPane().add(applet)
        applet.init
        frame.pack
        frame.setVisible(true)
    }

}