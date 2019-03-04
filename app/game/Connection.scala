package game

case class Connection(a: Int, b: Int,
                      midpoints: Seq[(Float, Float)] = Nil,
                      bz: Boolean = false,
                      tension: Float = 0)
