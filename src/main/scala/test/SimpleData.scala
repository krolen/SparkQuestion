package test

/**
  * Created by kkulagin1 on 2017-06-02.
  */
case class SimpleData(name1: String, value1: String)

object SimpleData  {
  import argonaut.Argonaut._
  import argonaut.CodecJson

  implicit def simpleDataJson: CodecJson[SimpleData] = {
    casecodec2(SimpleData.apply, SimpleData.unapply)(
      "name",
      "value"
    )
  }

}
