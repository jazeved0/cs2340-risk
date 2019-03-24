package models

/**
  * Connection DTO
  * @param a The index of the first node
  * @param b The index of the second node (no particular order)
  * @param midpoints Optionally, a list of tuples defining points to render a
  *                  polyline between the nodes' centers
  * @param bz Whether or not the connection should be a bezier curve
  * @param tension The tension of the line. 0 is straight, 1+ starts looking weird,
  *                values between 0.2 and 0.8 generally look best
  */
case class Connection(a: Int, b: Int,
                      midpoints: Seq[(Float, Float)] = Nil,
                      bz: Boolean = false,
                      tension: Float = 0)
