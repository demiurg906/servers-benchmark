package ru.hse.spb.runner.gui

import ru.hse.spb.common.ServerAddresses
import ru.hse.spb.runner.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import javax.swing.*
import javax.swing.border.LineBorder

typealias Fields = Pair<JFormattedTextField, List<JFormattedTextField>>

fun List<JComponent>.enable() = forEach { it.isEnabled = true }
fun List<JComponent>.disable() = forEach { it.isEnabled = false }

private enum class ValueType(val label: String, val minValue: Int) {
    N("N", 100),
    M("M", 1),
    DELTA("Delta", 0),
    X("X", 1)
}

class Gui {
    companion object {
        const val STATISTICS_FILE = "statistics"

        private const val WIDTH = 800
        private const val HEIGHT = 500

        private const val ERROR_WIDTH = 400
        private const val ERROR_HEIGHT = 200

        private val ipRegex = Regex("\\d\\d?\\d?.\\d\\d?\\d?.\\d\\d?\\d?.\\d\\d?\\d?")
    }

    private fun JFormattedTextField.initValue(value: Int) = this.apply { text = value.toString() }
    private fun JFormattedTextField.initValue(value: String) = this.apply { text = value }

    private val nField = JFormattedTextField().initValue(10_000)
    private val nStart = JFormattedTextField().initValue(1000)
    private val nEnd = JFormattedTextField().initValue(50_000)
    private val nStep = JFormattedTextField().initValue(10_000)
    private val nFields: Fields = nField to listOf(nStart, nEnd, nStep)

    private val mField = JFormattedTextField().initValue(5)
    private val mStart = JFormattedTextField().initValue(1)
    private val mEnd = JFormattedTextField().initValue(50)
    private val mStep = JFormattedTextField().initValue(10)
    private val mFields: Fields = mField to listOf(mStart, mEnd, mStep)

    private val deltaField = JFormattedTextField().initValue(1)
    private val deltaStart = JFormattedTextField().initValue(0)
    private val deltaEnd = JFormattedTextField().initValue(10)
    private val deltaStep = JFormattedTextField().initValue(1)
    private val deltaFields: Fields = deltaField to listOf(deltaStart, deltaEnd, deltaStep)

    private val xField = JFormattedTextField().initValue(5)
    private val serverAddressField = JFormattedTextField().initValue("127.0.0.1")
//    private val serverAddressField = JFormattedTextField().initValue("192.168.1.151")
    private val statisticsFileField = JFormattedTextField().initValue(STATISTICS_FILE)


    private var serverType: ServerType = ServerType.DUMMY
    private var rangeType: RangeType = RangeType.N_RANGE
        set(value) {
            field = value
            when (value) {
                RangeType.N_RANGE -> {
                    nFields.enableSecond()
                    mFields.enableFirst()
                    deltaFields.enableFirst()
                }

                RangeType.M_RANGE -> {
                    nFields.enableFirst()
                    mFields.enableSecond()
                    deltaFields.enableFirst()
                }

                RangeType.DELTA_RANGE -> {
                    nFields.enableFirst()
                    mFields.enableFirst()
                    deltaFields.enableSecond()
                }
            }
        }

    fun runApplication(width: Int = WIDTH, height: Int = HEIGHT) {
        createFrame(width, height).apply {
            isVisible = true
        }
    }

    private fun Fields.enableFirst() {
        first.isEnabled = true
        second.disable()
    }

    private fun Fields.enableSecond() {
        first.isEnabled = false
        second.enable()
    }

    private fun createFrame(width: Int, height: Int): JFrame = JFrame().apply {
        title = "Server benchmark"
        layout = BorderLayout()
        preferredSize = Dimension(width, height)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        add(createConfigPanel())
        add(createRunButton(), BorderLayout.SOUTH)

        pack()
        rangeType = RangeType.N_RANGE
    }

    private fun createConfigPanel(): JPanel = JPanel().apply {
        layout = GridLayout(2, 4)
        add(createServerTypePanel())
        add(createRangeTypePanel())
        add(createPanelWithConfigs("Choose N", nFields))
        add(createPanelWithConfigs("Choose M", mFields))
        add(createPanelWithConfigs("Choose Delta (ms)", deltaFields))
        add(createPanelWithX())

    }

    private fun createServerTypePanel(): JPanel = JPanel().apply {
        layout = GridLayout(3, 2)
        createBorder()

        val buttonGroup = ButtonGroup()

        fun JRadioButton.addToPanel() = this.also {
            buttonGroup.add(it)
            this@apply.add(it)
        }

        add(JLabel("Dummy server"))
        JRadioButton().apply {
            isSelected = true
            addActionListener { serverType = ServerType.DUMMY }
        }.addToPanel()

        add(JLabel("Smarter server"))
        JRadioButton().apply {
            isSelected = false
            addActionListener { serverType = ServerType.SMARTER }
        }.addToPanel()

        add(JLabel("Smartest server"))
        JRadioButton().apply {
            isSelected = false
            addActionListener { serverType = ServerType.SMARTEST }
        }.addToPanel()
    }

    private fun JPanel.createBorder() {
        border = LineBorder(Color.BLACK)
    }

    private fun createRangeTypePanel(): JPanel = JPanel().apply {
        layout = GridLayout(3, 2)
        createBorder()

        val buttonGroup = ButtonGroup()

        fun JRadioButton.addToPanel() = this.also {
            buttonGroup.add(it)
            this@apply.add(it)
        }

        add(JLabel("Change N"))
        JRadioButton().apply {
            isSelected = true
            addActionListener { rangeType = RangeType.N_RANGE }

        }.addToPanel()

        add(JLabel("Change M"))
        JRadioButton().apply {
            isSelected = false
            addActionListener { rangeType = RangeType.M_RANGE }
        }.addToPanel()

        add(JLabel("Change Delta"))
        JRadioButton().apply {
            isSelected = false
            addActionListener { rangeType = RangeType.DELTA_RANGE }
        }.addToPanel()
    }

    private fun createPanelWithConfigs(name: String, fields: Fields) = JPanel().apply {
        layout = GridLayout(4, 1)
        createBorder()
        add(JLabel(name))
        add(JLabel())
        fun addField(field: JTextField, label: String) {
            add(JPanel().apply {
                layout = GridLayout(1, 2)
                add(JLabel(label))
                add(field)
            })
        }

        addField(fields.first, "value")
        fields.second.zip(listOf("start", "stop", "step"))
            .forEach { addField(it.first, it.second) }
    }

    private fun createPanelWithX(): JPanel = JPanel().apply {
        layout = GridLayout(3, 2)
        createBorder()
        add(JLabel("X value"))
        add(xField)
        add(JLabel("Server address"))
        add(serverAddressField)
        add(JLabel("Output file"))
        add(statisticsFileField)
    }

    private fun createRunButton(): JButton = JButton().apply {
        text = "Run"
        addActionListener {
            isEnabled = false
            try {
                val config = collectConfig()
                ServerAddresses.serverAddress = getServerAddress()
                val summaryStatistic = collectStatistic(config, serverType)
                val statisticsFile = statisticsFileField.text.ifEmpty { STATISTICS_FILE } + ".csv"
                summaryStatistic.saveToCsv(File(statisticsFile))
                isEnabled = true
                GraphForm(readCsv(statisticsFile)).createFrame()
            } catch (e: FormatException) {
                createErrorFrame(this, e.message)
            }
        }
    }

    private fun createErrorFrame(button: JButton, message: String?) = JFrame().apply {
        val masterFrame = SwingUtilities.getWindowAncestor(button)
        val frame = this
        masterFrame.isEnabled = false

        add(JPanel().apply {
            layout = GridLayout(3, 1)
            add(JLabel("ERROR"))
            add(JLabel(message))
            add(JButton().apply {
                text = "OK"
                addActionListener {
                    frame.isVisible = false
                }
            })
        })
        setLocationRelativeTo(null)
        preferredSize = Dimension(ERROR_WIDTH, ERROR_HEIGHT)
        pack()
        addComponentListener(object : ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent?) {
                button.isEnabled = true
                masterFrame.isEnabled = true
            }
        })
        isVisible = true
    }

    private fun getServerAddress(): String {
        val address = serverAddressField.text
        if (address == null || address != "localhost" && !address.matches(ipRegex)) {
            throw FormatException("Incorrect ip: $address")
        }
        return address
    }

    private fun collectConfig(): Config = when (rangeType) {
        RangeType.N_RANGE -> NConfig(
            nFields.getProgression(ValueType.N),
            mField.getParameterValue(ValueType.M),
            deltaField.getParameterValue(ValueType.DELTA),
            xField.getInt()
        )
        RangeType.M_RANGE -> MConfig(
            nField.getParameterValue(ValueType.N),
            mFields.getProgression(ValueType.M),
            deltaField.getParameterValue(ValueType.DELTA),
            xField.getInt()
        )
        RangeType.DELTA_RANGE -> DeltaConfig(
            nField.getParameterValue(ValueType.N),
            mField.getParameterValue(ValueType.M),
            deltaFields.getProgression(ValueType.DELTA),
            xField.getInt()
        )
    }

    private fun Fields.getProgression(valueType: ValueType): IntProgression {
        val list = second.map { it.getInt() }

        val start = list[0]
        val end = list[1]
        val step = list[2]

        val minStart = valueType.minValue
        if (start < minStart) {
            throw FormatException("${valueType.label} start must be at least $minStart")
        }
        if (step <= 0) {
            throw FormatException("${valueType.label} step must be grater than zero")
        }
        if (end < start) {
            throw FormatException("${valueType.label} end value must be greater than start value")
        }

        return IntProgression.fromClosedRange(start, end, step)
    }

    private fun JFormattedTextField.getParameterValue(valueType: ValueType): Int {
        val value = getInt()
        val minValue = valueType.minValue
        if (value < minValue) {
            throw FormatException("${valueType.label} value must be at least $minValue")
        }
        return value
    }

    private fun JFormattedTextField.getInt(): Int = try {
        text.toInt()
    } catch (e: NumberFormatException) {
        throw FormatException("\"$text\" is not an integer")
    }
}

fun main(args: Array<String>) {
    Gui().runApplication()
}