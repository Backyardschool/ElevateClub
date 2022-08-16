package com.patson.model

case class Log(airline : Airline, message : String, category : LogCategory.Value, severity : LogSeverity.Value, cycle : Int, properties : Map[String, String] = Map.empty)

object LogCategory extends Enumeration {
    type LogCategory = Value
    val LINK, NEGOTIATION, AIRPORT_RANK_CHANGE = Value
    
    val getDescription : LogCategory.Value => String = {
      case LINK => "Flight Route"
      case NEGOTIATION => "Negotiation"
      case AIRPORT_RANK_CHANGE => "Airport Rank Change"
    }
}

object LogSeverity extends Enumeration {
    type LogSeverity = Value
    val WARN, INFO, FINE = Value
    
    val getDescription : LogSeverity.Value => String = {
      case WARN => "Warning"
      case INFO => "Information"
      case FINE => "Fine details"
    }
}

