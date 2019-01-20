package ru.hse.spb.runner.gui

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.JPanel

class GraphForm(private val series: Series) {
    companion object {
        private const val WIDTH = 600
        private const val HEIGHT = 600
    }

    fun createFrame(width: Int = WIDTH, height: Int = HEIGHT, title: String = ""): JFrame = JFrame().apply {
        preferredSize = Dimension(width, height)
        this.title = title
        setLocationRelativeTo(null)
        add(createChartPanel())
        pack()
        isVisible = true
    }

    private fun createChartPanel(): JPanel = ChartPanel(createChart())

    private fun createChart(): JFreeChart {
        val dataset = XYSeriesCollection().apply {
            addSeries(this@GraphForm.series.clientTime)
            addSeries(this@GraphForm.series.serverTime)
            addSeries(this@GraphForm.series.sortingTime)
        }

        return ChartFactory.createScatterPlot(
            "Statistics",
            series.xLabel,
            series.yLabel,
            dataset
        ).apply {
            xyPlot.renderer = XYLineAndShapeRenderer().apply {
                setSeriesLinesVisible(0, true)
                setSeriesLinesVisible(1, true)
                setSeriesLinesVisible(2, true)
            }
        }
    }
}

data class Series(
    val xLabel: String,
    val yLabel: String,
    val clientTime: XYSeries,
    val serverTime: XYSeries,
    val sortingTime: XYSeries
)

fun readCsv(fileName: String): Series =
    CSVParser(File(fileName).bufferedReader(), CSVFormat.DEFAULT.withFirstRecordAsHeader()).use { parser ->
        val headerMap = parser.headerMap.map { (key, value) -> value to key }.toMap()
        val xLabel = headerMap[0] ?: throw IllegalStateException()
        val sortingTime = XYSeries(headerMap[1] ?: throw IllegalStateException())
        val serverTime = XYSeries(headerMap[2] ?: throw IllegalStateException())
        val clientTime = XYSeries(headerMap[3] ?: throw IllegalStateException())
        for (record in parser.records) {
            val x = record[0].toInt()
            sortingTime.add(x, record[1].toDouble())
            serverTime.add(x, record[2].toDouble())
            clientTime.add(x, record[3].toDouble())
        }
        Series(xLabel, "seconds", clientTime, serverTime, sortingTime)
    }

fun main(args: Array<String>) {
    GraphForm(readCsv(Gui.STATISTICS_FILE)).createFrame()
}