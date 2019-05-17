package controllers.format

import play.api.libs.json.{JsObject, JsString, JsValue, Writes}

/**
  * Writes a generic String -> Any map to JSON
  */
class PayloadWrites extends Writes[Seq[(String, Any)]] {
  override def writes(data: Seq[(String, Any)]): JsValue = {
    JsObject(data.iterator
      .map { case (key, value) => (key, JsString(value.toString)) }.toList)
  }
}
