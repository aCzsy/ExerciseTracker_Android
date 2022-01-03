package com.example.exercise_tracker

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import java.text.DecimalFormat

/**
 * This class represents a CustomView which is a Line graph that shows speed/distance results.
 * Y axis: Speed in km/h
 * X axis: Distance in km
 */

class TrackerStatsView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    //Declaring required variables
    private var size = width
    private var count = 0
    private var graphGrid = Array(12) {Array(12) {GraphCell(0,0,0,0,0,0)} }
    private var graph_background_color = Color.rgb(22, 27, 29)
    private var _strokeWidth = 6.0f //stroke width
    private var path = Path() //Path used to draw lines
    private var points = arrayListOf<Double>() //Graph points
    private var distances = arrayListOf<Double>() //x axis distances

    //Paint objects
    @Transient
    private val graph_background_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = graph_background_color
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = _strokeWidth
    }

    @Transient
    private val axis_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(145, 118, 8)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = _strokeWidth
    }

    @Transient
    private val point_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 208, 61)
        style = Paint.Style.STROKE
        strokeWidth = _strokeWidth
    }

    @Transient
    private val graph_text_paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 35.0f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create( "", Typeface.BOLD)
        color = Color.rgb(110, 135, 145)
    }

    override fun onDraw(canvas: Canvas) {
        count++
        super.onDraw(canvas)
        //Operations inside this if statement are only executed once
        if(count == 1){
            initializeGrid()
            points = convertSpeeds(points)
        }
        rescale()

        drawBackground(canvas)
        drawGrid(canvas)
        drawLines(canvas)
        drawGraph(canvas)
        drawText(canvas)
    }

    /**
     * This method draws a full width rectangle which serves as a graph background
     * @param canvas: Canvas object that the background is drawn on
     */
    private fun drawBackground(canvas: Canvas){
        canvas.save()
        val r = Rect(0,0,width,width)
        canvas.drawRect(r,graph_background_paint)
        canvas.restore()
    }

    /**
     *This method initializes a grid which represents a graph paper
     * Graph grid is 12x12, but only 10x10 are used for drawing points on
     * Other cells are used for displaying metrics text
     */
    private fun initializeGrid(){
        var cellWidth = width / 12; //width of each graph cell based on the custom view width (view width/ 12)
        //initializing each item as a GraphCell with its own position and dimensions
        for(i:Int in 0 until graphGrid.size) {
            for(j : Int in 0 until graphGrid[i].size) {
                var pos_x = i*cellWidth
                var pos_y = j*cellWidth
                graphGrid[i][j] = GraphCell(i,j,pos_x,pos_y,pos_x+cellWidth,pos_y+cellWidth)
            }
        }
    }

    /**
     * This method takes care of rescaling the grid scale ratio if the CustomView width changes
     */
    private fun rescale(){
        var cellWidth = size / 12; //width of each cell based on the custom view width
        //only changing dimensions of each cell using its index in the grid
        for(i:Int in 0 until graphGrid.size) {
            for(j : Int in 0 until graphGrid[i].size) {
                var pos_x = graphGrid[i][j].i*cellWidth
                var pos_y = graphGrid[i][j].j*cellWidth
                //Rect object(dimensionss of a cell) of each cell gets set with recalculated values
                graphGrid[i][j].graph_cell_rect = Rect(pos_x,pos_y,pos_x+cellWidth,pos_y+cellWidth)
            }
        }
    }

    /**
     * This method displays every graph cell on canvas
     * @param canvas: Canvas object that the grid is drawn on
     */
    private fun drawGrid(canvas: Canvas){
        for (i in 0 until graphGrid.size) {
            for (j in 0 until graphGrid[i].size) {
                //Only showing cells that are used to plot points on
                if(i > 0 && i < graphGrid.size-1 && j > 0 && j < graphGrid[i].size-1){
                    graphGrid[i][j].show(canvas)
                }
            }
        }
    }

    /**
     * This method displays y axis speed units and x axis distance units
     * @param canvas: Canvas object that the text is drawn on
     */
    private fun drawText(canvas: Canvas){
        //DecimalFormat object is used to format distance values to 2 decimal points
        val df = DecimalFormat("0.00")
        var centerToWall = (width/12)/2 //Distance from cell's wall to the center (or center to the wall)
        //Log.wtf("DISTANCES",distances.size.toString())
        //Looping through the length of a grid
        for (i in 0 until graphGrid.size-1) {
            //And displaying speed units on the y axis from 10 to 0
            canvas.drawText((10-i).toString(),graphGrid[0][i].centerXofCell().toFloat(),graphGrid[0][i].centerYofCell()+55.toFloat(),graph_text_paint)
            //Only showing x axis units if 2 or more distance points were tracked, otherwise 0.0km is shown
            //Only 3 values are shown on the graph(first,middle,last)
            if(distances.size >= 2 && distances.size >= i){
                if(i == 0){
                    var el1 = df.format(distances.get(0)) + "km"
                    canvas.drawText(el1,graphGrid[0][graphGrid.size-1].centerXofCell().toFloat()+(centerToWall*2).toFloat(),graphGrid[0][graphGrid.size-1].centerYofCell().toFloat()+20f,graph_text_paint)
                }
                if(i == 4){
                    var el2 = df.format((distances.get(distances.size/2))) + "km"
                    canvas.drawText(el2,graphGrid[4][graphGrid.size-1].centerXofCell().toFloat()+(centerToWall*2).toFloat(),graphGrid[0][graphGrid.size-1].centerYofCell().toFloat()+20f,graph_text_paint)
                }
                else if(i == 9){
                    var el3 = df.format(distances.get(distances.size-1)) + "km"
                    canvas.drawText(el3,graphGrid[9][graphGrid.size-1].centerXofCell().toFloat()+(centerToWall*2).toFloat(),graphGrid[0][graphGrid.size-1].centerYofCell().toFloat()+20f,graph_text_paint)
                }
            }
            else if(distances.size<2){
                canvas.drawText("0.0km",graphGrid[0][graphGrid.size-1].centerXofCell().toFloat()+(centerToWall*2).toFloat(),graphGrid[0][graphGrid.size-1].centerYofCell().toFloat()+20f,graph_text_paint)
            }
        }
    }

    /**
     * This method is responsible for drawing x and y axis lines
     * @param canvas: Canvas object that the lines are drawn on
     */
    fun drawLines(canvas: Canvas){
        canvas.drawLine(((width/12)).toFloat(),((width/12)).toFloat()+0f, ((width/12)).toFloat(), height.toFloat()-(width/12), axis_paint);//y-axis
        canvas.drawLine(((width/12)).toFloat(), height.toFloat()-(width/12),
            width.toFloat()-(width/12), width.toFloat()-(width/12), axis_paint);//x-axis
    }

    /**
     * This method plots the speed points on the graph and connects these points with lines
     * Graph only shows 10 points during the journey, so if number of speed points tracked is more than 10,
     * a new list is created which stores 10 points from the original list that are chosen at intervals
     * which depend on the length of the original list and that list is then plotted.
     * @param canvas: Canvas object that the grapgh is drawn on
     */
    private fun drawGraph(canvas: Canvas){
        //List that is used if number of tracked speed points if larger than 10
        var new_points = arrayListOf<Double>()

        //If number of tracked points is greater than 10
        if(points.size > 10){
            var i = 0 //current index
            var lastI = 0 //last index
            //looping through the list
            while (i < points.size) {
                //adding first element to the new list
                new_points.add(points.get(i))
                lastI = i //saving previous element
                //Calculating next pointer position based on the original speed points list
                i += Math.ceil(points.size/10.0).toInt()
            }
            //In a situation where size of original list was for example 21
            //i = Math.ceil(21/10).toInt() = 3
            //elements at indexes that would be added to new list: 0,3,6,9,12,15,18,21 which is total of 8 elements
            //but we need 10 elements to show, so we loop from last i position until the end
            //and add remaining elements
            if(lastI < points.size) {
                while (lastI < points.size && new_points.size < 10) {
                    new_points.add(points.get(lastI))
                    lastI++
                }
            }
            //setting original points list to the new list
            points = new_points
        }

        //In any case, the size of the list of points that are to be plotted is 10 now
        //pointers
        var pointer = 1
        var point_index = 2
        var x_move2 = 1
        var element = 1
        var x_move = 0
        var y_move = 0
        var centerToWall = (width/12)/2
        //Loop to plot points on the graph
        for((index,value ) in points.withIndex()){
            //x position is at every next cell's middle point
            val pos_x = graphGrid[index+1][9-x_move].centerXofCell().toFloat()
            //y position always starts from the bottom line of the graph, then its height is calculated based on the speed point value and cell width
            val pos_y = graphGrid[index+1][9-y_move].centerYofCell().toFloat()+centerToWall.toFloat()-(value*(width/12)).toFloat()+(width/12)
            //drawing the point on the graph
            canvas.drawCircle(pos_x, pos_y, 5F, point_paint)
            x_move++
        }

        //setting starting point of the path from where the path will be drawn
        //starting from index 1 as index 0 is reserved for speed units text values
        path.moveTo(graphGrid[1][9].centerXofCell().toFloat(),graphGrid[1][9].centerYofCell().toFloat()+centerToWall.toFloat()-(points.get(0)*(width/12)).toFloat()+(width/12))

        //Positions are calculated for all elements in the list
        while(pointer < points.size){
            val pos_x = graphGrid[point_index][9-x_move2].centerXofCell().toFloat()
            val pos_y = graphGrid[point_index][9-y_move].centerYofCell().toFloat()+centerToWall.toFloat()-(points.get(element)*(width/12)).toFloat()+(width/12)
            path.lineTo(pos_x,pos_y)
            pointer++
            point_index++
            element++
        }
        //drawing full path
        canvas.drawPath(path,point_paint)
    }

    /**
     * This method takes all tracked speeds which are in m/s
     * and converts them to km/h in ratio 1 m/s = 3.6 km/h
     * Because the maximum speed is set to 10 km/h, all converted speeds that are above that value, are set to 10.0
     * @param _speeds: List of speeds in m/s
     * @return List of speeds in km/h
     */
    private fun convertSpeeds(_speeds:ArrayList<Double>):ArrayList<Double>{
        var converted = 0.0
        var newArr: ArrayList<Double> = ArrayList()

        for(i in _speeds){
            converted = (i * 0.0036) * 1000
            if(converted > 10.0){
                newArr.add(10.0)
            } else {
                newArr.add(converted)
            }
        }
        return newArr
    }

    /**
     * Setter that is used by StatsActivity
     */
    public fun setPoints(_points:ArrayList<Double>){
        this.points = _points
    }

    /**
     * Setter that is used by StatsActivity
     */
    fun setDistances(_distances:ArrayList<Double>){
        this.distances = _distances
    }

    /**
     * Saving variables' states
     */
    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putInt("count",count)
        bundle.putSerializable("graphGrid",graphGrid)
        bundle.putSerializable("points",points)
        bundle.putSerializable("distances",distances)
        bundle.putParcelable("superState", super.onSaveInstanceState())
        return bundle
    }

    /**
     * Restoring variables' states
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        var viewState = state
        if (viewState is Bundle) {
            count = viewState.getInt("count")
            graphGrid = viewState.getSerializable("graphGrid") as Array<Array<GraphCell>>
            points = viewState.getSerializable("points") as ArrayList<Double>
            distances = viewState.getSerializable("distances") as ArrayList<Double>
            viewState = viewState.getParcelable("superState")
        }
        super.onRestoreInstanceState(viewState)
    }

    /**
     * Function that makes the view responsive and fit its parent
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        size = Math.min(measuredWidth,measuredHeight)
        setMeasuredDimension(size,size)
    }
}