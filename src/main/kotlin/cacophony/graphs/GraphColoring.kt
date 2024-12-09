package cacophony.graphs

interface GraphColoring<VertexT, ColorT> {
    fun doColor(
        graph: Map<VertexT, Collection<VertexT>>,
        coalesce: Map<VertexT, Collection<VertexT>>,
        fixedColors: Map<VertexT, ColorT>,
        allowedColors: Collection<ColorT>,
    ): Map<VertexT, ColorT>
}
