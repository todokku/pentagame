package koolui

import com.lightningkite.koolui.layout.Layout
import com.lightningkite.koolui.layout.views.LayoutViewWrapper
import com.lightningkite.koolui.layout.views.wrap
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import io.data2viz.viz.JFxVizRenderer
import io.data2viz.viz.Viz
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import mu.KotlinLogging

interface LayoutJavaFxData2Viz : ViewFactoryData2Viz<Layout<*, Node>>, /*HasScale,*/ LayoutViewWrapper<Node> {
    companion object{
        private val logger = KotlinLogging.logger {}
    }

    class SquareCanvas: Canvas(600.0 ,600.0) {
        var onResize: ()->Unit = {}
        override fun isResizable(): Boolean = true
        override fun resize(width: Double, height: Double) {
            val side = if (height > width) width else height
            super.setWidth(side)
            super.setHeight(side)
            onResize()
        }
    }
    override fun vizCanvas(viz: ObservableProperty<Viz>): Layout<*, Node> {
        return wrap(SquareCanvas()) { lifecycle ->
            // TODO: resize only in square ?
            lifecycle.bind(viz){ viz ->
                logger.info("initalizing viz")
                JFxVizRenderer(
                    canvas = this,
                    viz = viz
                )
                onResize = {
                    viz.resize(width, height)
                    viz.render()
                }
//                postSetup(viz)
//                viz.render()
            }
        }
    }
}