package controllers

/**
  * Response type to Request-type format
  */
object RequestResponse extends Enumeration {
  type Response = Value
  val Accepted, Rejected = Value
}
