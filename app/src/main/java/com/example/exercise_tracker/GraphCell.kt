package com.example.exercise_tracker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable


/**
 * This class represents a Cell on the Graph paper
 *
 */
class GraphCell(var i:Int, var j:Int, var l:Int, var r: Int, var t:Int, var b:Int):Parcelable, Serializable {
    private var _i:Int = i //row index of 2d Array
    private var _j:Int = j //column index of 2d Array
    private var _l:Int = l //left value of Rect
    private var _r:Int = r //right value of Rect
    private var _t:Int = t //top value of Rect
    private var _b:Int = b //bottom value of Rect

    private var cellOutlineColor = Color.rgb(97, 79, 5)
    private var _strokeWidth = 1.0f

    //Paint objects
    //Annotated as @Transient so that they're not considered for parcelization/serialization by the system
    @Transient
    private val graph_cell_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }
    @Transient
    private val stroke_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cellOutlineColor
        style = Paint.Style.STROKE
        strokeWidth = _strokeWidth
    }

    //Rect object of a graph cell
    //Annotated as @Transient so that it's not considered for parcelization/serialization by the system
    @Transient
    var graph_cell_rect = Rect(_l,_r,_t,_b)

    //Required for Parcelization
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
        _i = parcel.readInt()
        _j = parcel.readInt()
        _l = parcel.readInt()
        _r = parcel.readInt()
        _t = parcel.readInt()
        _b = parcel.readInt()
        cellOutlineColor = parcel.readInt()
        _strokeWidth = parcel.readFloat()
    }


    /**
     * Public function that displays a cell on the graph grid
     * @param __canvas: Canvas object which is used to draw a cell on
     */
    fun show(__canvas: Canvas){
        this.display_graph_cell(__canvas)
    }

    /**
     * Private function that draws a Rect with set parameters
     * @param __canvas: Canvas object which is used to draw a cell on
     */
    private fun display_graph_cell(__canvas: Canvas){
        drawCellRect(__canvas,graph_cell_paint,stroke_paint,graph_cell_rect)
    }

    /**
     * Private function that takes all paramaters needed for drawing a Rect and draws it
     * @param __canvas: Canvas object which is used to draw a cell on
     * @param cellPaint: Paint object
     * @param cellRect: Rect to be drawn
     */
    private fun drawCellRect(__canvas: Canvas,cellPaint: Paint,strokePaint:Paint,cellRect: Rect){
        __canvas.save()
        __canvas.drawRect(cellRect,cellPaint)
        __canvas.restore()
        __canvas.save()
        __canvas.drawRect(cellRect,strokePaint)
        __canvas.restore()
    }

    /**
     * Center X position of a cell
     */
    public fun centerXofCell(): Int {
        return graph_cell_rect.centerX()
    }

    /**
     * Center Y position of a cell
     */
    public fun centerYofCell():Int{
        return graph_cell_rect.centerY()
    }

    /**
     * top value of the Rect object of a cell
     */
    fun getTop(): Int {
        return this._t
    }

    /**
     * Required for Parcelization
     * Flatten this object in to a Parcel.
     * @param parcel: The Parcel in which the object should be written
     * @param flags: Additional flags about how the object should be written
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(i)
        parcel.writeInt(j)
        parcel.writeInt(l)
        parcel.writeInt(r)
        parcel.writeInt(t)
        parcel.writeInt(b)
        parcel.writeInt(_i)
        parcel.writeInt(_j)
        parcel.writeInt(_l)
        parcel.writeInt(_r)
        parcel.writeInt(_t)
        parcel.writeInt(_b)
        parcel.writeInt(cellOutlineColor)
        parcel.writeFloat(_strokeWidth)
    }

    /**
     * Required for Parcelization
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled representation
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Required for Parcelization
     * Interface that must be implemented and provided as a public CREATOR field that generates instances of Parcelable class from a Parcel
     */
    companion object CREATOR : Parcelable.Creator<GraphCell> {
        override fun createFromParcel(parcel: Parcel): GraphCell {
            return GraphCell(parcel)
        }

        override fun newArray(size: Int): Array<GraphCell?> {
            return arrayOfNulls(size)
        }
    }

}