package no.iktdev.kammich.system.network.parser

class WifiPhyInfoParser {

    data class WifiCapability(
        val supportsAP: Boolean,
        val isConcurrent: Boolean,
        val sameChannelConstraint: Boolean
    )

    fun parse(output: String): WifiCapability {
        // Renser input for konsistent parsing
        val cleanOutput = output.replace("\t", "    ")
        val tree = parseToTree(cleanOutput)

        val modesNode = tree.findCategory("Supported interface modes:")
        val supportsAP = modesNode?.children?.any { it.text.contains("AP") } ?: false

        val combNode = tree.findCategory("valid interface combinations:")
        val isConcurrent = combNode?.children?.any {
            it.text.contains("managed") && it.text.contains("AP")
        } ?: false

        val sameChannel = combNode?.children?.any {
            it.text.contains("managed") && it.text.contains("AP") && it.text.contains("channels <= 1")
        } ?: false

        return WifiCapability(supportsAP, isConcurrent, sameChannel)
    }

    private fun parseToTree(output: String): IwNode {
        val root = IwNode("ROOT")
        val stack = mutableListOf(Pair(-1, root))

        output.lineSequence().filter { it.isNotBlank() }.forEach { rawLine ->
            val indent = rawLine.takeWhile { it == ' ' }.length
            val text = rawLine.trim()
            val newNode = IwNode(text)

            while (stack.isNotEmpty() && stack.last().first >= indent) {
                stack.removeLast()
            }

            stack.last().second.children.add(newNode)
            stack.add(Pair(indent, newNode))
        }
        return root
    }

    // Intern node-struktur
    private class IwNode(val text: String) {
        val children = mutableListOf<IwNode>()

        fun findCategory(prefix: String): IwNode? {
            if (text.startsWith(prefix)) return this
            for (child in children) {
                val found = child.findCategory(prefix)
                if (found != null) return found
            }
            return null
        }
    }
}