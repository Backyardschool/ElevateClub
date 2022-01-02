package com.patson.model

import com.patson.data.CountrySource
import com.patson.model.airplane.Model

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, zone : String, var size : Int, baseIncome : Int, basePopulation : Long, var runwayLength : Int = Airport.MIN_RUNWAY_LENGTH, var id : Int = 0) extends IdObject {
  val citiesServed = scala.collection.mutable.ListBuffer[(City, Double)]()
  private[this] val airlineBaseAppeals = new java.util.HashMap[Int, AirlineAppeal]() //base appeals
  private[this] val airlineAdjustedAppeals = new java.util.HashMap[Int, AirlineAppeal]() //base appeals + bonus
  private[this] val allAirlineBonuses = new java.util.HashMap[Int, List[AirlineBonus]]() //bonus appeals
  private[this] var airlineAppealsLoaded = false
//  private[this] val slotAssignments = scala.collection.mutable.Map[Int, Int]()
//  private[this] var slotAssignmentsLoaded = false
  private[this] val airlineBases = scala.collection.mutable.Map[Int, AirlineBase]()
  private[this] var airlineBasesLoaded = false
  private[this] val baseFeatures = ListBuffer[AirportFeature]()
  private[this] var featuresLoaded = false
  private[this] var projectsLoaded = false
  private[this] val loungesByAirline = scala.collection.mutable.Map[Int, Lounge]()
  private[this] val loungesByAlliance = scala.collection.mutable.Map[Int, Lounge]()
  private[this] var assets = List[AirportAsset]()



  private[this] var runways = List.empty[Runway]
  //private[this] var loungesLoaded = false

//  private[this] var airportImageUrl : Option[String] = None
//  private[this] var cityImageUrl : Option[String] = None

  private[model] var country : Option[Country] = None

  //val baseIncome = if (basePopulation > 0) (power / basePopulation).toInt  else 0


  private[this] var assetBoosts : Map[AirportBoostType.Value, List[AirportBoost]] = Map.empty

  val baseIncomeLevel = Computation.getIncomeLevel(baseIncome)
  lazy val incomeLevelBoost = assetBoosts.get(AirportBoostType.INCOME) match {
    case Some(boosts) => boosts.map(_.value).sum
    case None => 0
  }
  lazy val incomeLevel = baseIncomeLevel + incomeLevelBoost
  lazy val income = if (incomeLevelBoost == 0) baseIncome else Computation.fromIncomeLevel(incomeLevel) //have to deduce from income level (after boost)


  lazy val populationBoost = assetBoosts.get(AirportBoostType.POPULATION) match {
    case Some(boosts) => boosts.map(_.value).sum.toInt
    case None => 0
  }
  lazy val population = basePopulation + populationBoost
  lazy val power = income * population


  lazy val features : List[AirportFeature] = computeFeatures()


//  def availableSlots : Int = {
//    if (slotAssignmentsLoaded) {
//      slots - slotAssignments.foldLeft(0)(_ + _._2)
//    } else {
//      throw new IllegalStateException("airline slot assignment is not properly initialized! If loaded from DB, please use fullload")
//    }
//  }

  lazy val rating =  AirportRating.rateAirport(this)

  def addCityServed(city : City, share : Double) {
    citiesServed += Tuple2(city, share)
  }

  def getAirlineAdjustedAppeals() : Map[Int, AirlineAppeal] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAdjustedAppeals.asScala.toMap
  }

  def getAirlineBaseAppeals() : Map[Int, AirlineAppeal] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineBaseAppeals.asScala.toMap
  }

  def getAirlineBaseAppeal(airlineId : Int) : AirlineAppeal = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val result = airlineBaseAppeals.get(airlineId)
    if (result != null) {
      result
    } else {
      AirlineAppeal(0, 0)
    }
  }

  def setRunways(runways : List[Runway]) = {
    this.runways = runways
    if (runways.length > 0) {
      val maxRunwayLength = runways.sortBy(_.length).reverse.head.length
      this.runwayLength = maxRunwayLength.toInt
    }
    if (this.runwayLength < Airport.MIN_RUNWAY_LENGTH) {
      this.runwayLength = Airport.MIN_RUNWAY_LENGTH
    }
  }

  def getRunways() = {
    runways
  }

  def getAirlineBonuses(airlineId : Int) : List[AirlineBonus] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    allAirlineBonuses.asScala.getOrElse(airlineId, List.empty)
  }

  def getAllAirlineBonuses() : Map[Int, List[AirlineBonus]] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    allAirlineBonuses.asScala.toMap
  }
  
//  def setAirlineBaseLoyalty(airlineId : Int, value : Double) = {
//    if (!airlineAppealsLoaded) {
//      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
//    }
//    val oldAppeal = airlineBaseAppeals.getOrDefault(airlineId, AirlineAppeal(0, 0))
//    airlineBaseAppeals.put(airlineId, AirlineAppeal(value, oldAppeal.awareness))
//  }
//  def setAirlineBaseAwareness(airlineId : Int, value : Double) = {
//    if (!airlineAppealsLoaded) {
//      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
//    }
//    val oldAppeal = airlineAppeals.getOrDefault(airlineId, AirlineAppeal(0, 0))
//    airlineBaseAppeals.put(airlineId, AirlineAppeal(oldAppeal.loyalty, value))
//  }
  def getAirlineLoyalty(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val appeal = airlineAdjustedAppeals.get(airlineId)
    if (appeal != null) {
      appeal.loyalty
    } else {
      0
    }
  }
  def getAirlineAwareness(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val appeal = airlineAdjustedAppeals.get(airlineId)
    if (appeal != null) {
      appeal.awareness
    } else {
      0
    }
  }

  
  def isAirlineAppealsInitialized = airlineAppealsLoaded

  
  def getAirlineBase(airlineId : Int) : Option[AirlineBase] = {
    if (!airlineBasesLoaded) {
      throw new IllegalStateException("airport base is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineBases.get(airlineId)
  }
  
  def getAirlineBases() : Map[Int, AirlineBase] = {
    airlineBases.toMap
  }
  
  def getLoungeByAirline(airlineId : Int, activeOnly : Boolean = false) : Option[Lounge] = {
    loungesByAirline.get(airlineId).filter(!activeOnly || _.status == LoungeStatus.ACTIVE)
  }
  
  def getLounges() : List[Lounge] = {
    loungesByAirline.values.toList
  }
  
  def getLoungeByAlliance(alliance : Int, activeOnly : Boolean = false) : Option[Lounge] = {
    loungesByAlliance.get(alliance).filter(!activeOnly || _.status == LoungeStatus.ACTIVE)
  }
  
  def getLounge(airlineId : Int, allianceIdOption : Option[Int], activeOnly : Boolean = false) : Option[Lounge] = {
     getLoungeByAirline(airlineId, activeOnly) match {
       case Some(lounge) => Some(lounge)
       case None => allianceIdOption match {
         case Some(allianceId) => getLoungeByAlliance(allianceId, activeOnly)
         case None => None
       }
     }
     
  }
  
  def isFeaturesLoaded = featuresLoaded
  
  def getFeatures() : List[AirportFeature] = {
    features.toList
  }

  def isGateway() = {
    baseFeatures.find(_.featureType == AirportFeatureType.GATEWAY_AIRPORT).isDefined
  }

  def initAirlineAppealsComputeLoyalty(airlineBaseAwareness : Map[Int, Double], airlineBonuses : Map[Int, List[AirlineBonus]] = Map.empty, loyalistEntries : List[Loyalist]) = {
    this.loyalistEntries = loyalistEntries
    val airlineBaseLoyalty = computeLoyaltyByLoyalist(loyalistEntries)

    val preFlattenAppealsByAirlineId : Map[Int, List[(Int, AirlineAppeal)]] = (airlineBaseLoyalty.view.mapValues(loyalty => AirlineAppeal(loyalty = loyalty, awareness = 0)).toList ++
    airlineBaseAwareness.view.mapValues(awareness => AirlineAppeal(loyalty = 0, awareness = awareness)).toList).groupBy(_._1)
    //now flatten it if both awareness and loyalty entry is present
    val appealsByAirlineId = preFlattenAppealsByAirlineId.view.mapValues { entries =>
      AirlineAppeal(loyalty = entries.map(_._2.loyalty).sum, awareness = entries.map(_._2.awareness).sum)
    }.toMap
    initAirlineAppeals(appealsByAirlineId, airlineBonuses)
  }

  private[model] lazy val computeLoyaltyByLoyalist = (loyalistEntries : List[Loyalist]) => loyalistEntries.map {
    case Loyalist(_, airline, amount) => {
      if (population == 0) { //should not happen, but just to be safe
        (airline.id, 0.0)
      } else {
        val loyalistRatio = amount.toDouble / population //to attain 100, it requires full conversion
        val baseLoyalty = Math.log10(1 + loyalistRatio * 9) * 100 // 0 -> 0, 1 -> 100
        (airline.id, Math.min(AirlineAppeal.MAX_LOYALTY, baseLoyalty))
      }
    }
  }.toMap

  
  def initAirlineAppeals(airlineBaseAppeals : Map[Int, AirlineAppeal], airlineBonuses : Map[Int, List[AirlineBonus]] = Map.empty) = {
    this.airlineBaseAppeals.clear()
    this.airlineBaseAppeals.asScala ++= airlineBaseAppeals

    this.airlineAdjustedAppeals.clear()
    this.airlineAdjustedAppeals.asScala ++= airlineBaseAppeals

    this.allAirlineBonuses.clear()
    airlineBonuses.foreach {
      case(airlineId, bonuses) =>
        allAirlineBonuses.put(airlineId, bonuses)
        //add the adjustments
        bonuses.foreach { bonus =>
          val existingAppeal = this.airlineAdjustedAppeals.get(airlineId)
          if (existingAppeal != null) {
            val newLoyalty = Math.min(existingAppeal.loyalty + bonus.bonus.loyalty, AirlineAppeal.MAX_LOYALTY)
            val newAwareness = Math.min(existingAppeal.awareness + bonus.bonus.awareness, AirlineAppeal.MAX_AWARENESS)
            this.airlineAdjustedAppeals.put(airlineId, AirlineAppeal(newLoyalty, newAwareness))
          } else { //not yet has appeal data, add one
            this.airlineAdjustedAppeals.put(airlineId, bonus.bonus)
          }
        }
    }

    airlineAppealsLoaded = true
  }
//  def initSlotAssignments(slotAssignments : Map[Int, Int]) = {
//    this.slotAssignments.clear()
//    this.slotAssignments ++= slotAssignments
//    slotAssignmentsLoaded = true
//  }
  def initAirlineBases(airlineBases : List[AirlineBase]) = {
    this.airlineBases.clear()
    airlineBases.foreach { airlineBase =>
      this.airlineBases.put(airlineBase.airline.id, airlineBase)
    }
    airlineBasesLoaded = true
  }
  def initFeatures(features : List[AirportFeature]) = {
    this.baseFeatures.clear()
    this.baseFeatures ++= features
    featuresLoaded = true
  }

  def initAssets(assets : List[AirportAsset]) = {
    this.assets = assets
    projectsLoaded = true
    assetBoosts = assets.flatMap(_.boosts).groupBy(_.boostType)
  }

  def addFeature(feature : AirportFeature) = {
    this.baseFeatures += feature
  }
  
  def initLounges(lounges : List[Lounge]) = {
    this.loungesByAirline.clear()
    lounges.foreach { lounge =>
      this.loungesByAirline.put(lounge.airline.id, lounge)
      lounge.allianceId.foreach {
         allianceId => this.loungesByAlliance.put(allianceId, lounge)  
      }
    }
  }
  
  def slotFee(airplaneModel : Model, airline : Airline) : Int = { 
    val baseSlotFee = size match {
      case 1 => 50 //small
      case 2 => 50 //medium
      case 3 => 80 //large
      case 4 => 150  //international class
      case 5 => 250
      case 6 => 350 
      case _ => 500 //mega airports - not suitable for tiny jets
    }
    
    import Model.Type._
    val multiplier = airplaneModel.airplaneType match {
      case LIGHT => 1
      case SMALL => 1
      case REGIONAL => 3
      case MEDIUM => 8
      case LARGE => 12
      case X_LARGE => 15
      case JUMBO => 18
      case SUPERSONIC => 12
    }
    
    //apply discount if it's a base
    val discount = getAirlineBase(airline.id) match {
      case Some(airlineBase) =>
        if (airlineBase.headquarter) 0.5 else 0.8 //headquarter 50% off, base 20% off
      case None =>
        1 //no discount
    }
    
    (baseSlotFee * multiplier * discount).toInt
  }
  
  def landingFee(airplaneModel : Model) : Int = {
    val perSeat = 
      if (size <= 3) {
        3
      } else {
        size
      }
    
    airplaneModel.capacity * perSeat
  }
  
  def allowsModel(airplaneModel : Model) : Boolean = {
    runwayLength >= airplaneModel.runwayRequirement
  }
  
  val expectedQuality = (flightType : FlightType.Value, linkClass : LinkClass) => {
    Math.max(0, Math.min(incomeLevel.toInt, 50) + Airport.qualityExpectationFlightTypeAdjust(flightType)(linkClass)) //50% on income level, 50% on flight adjust
  }
  
  private[this] def getCountry() : Country = {
    if (country.isEmpty) {
      country = CountrySource.loadCountryByCode(countryCode)
    }
    country.get
  }
  
  lazy val airportRadius : Int = {
    size match {
      case 1 => 100
      case 2 => 150
      case n if (n >= 3) => 250
      case _ => 0
    }
  }
  
  val displayText = city + "(" + iata + ")"

  var loyalistEntries : List[Loyalist] = List.empty

  def computeFeatures() = {
    val newFeatures = ListBuffer[AirportFeature]()
    assetBoosts.foreach {
      case(boostType, boosts) =>
        boostType match {
          case com.patson.model.AirportBoostType.INTERNATIONAL_HUB =>
            newFeatures.append(InternationalHubFeature(0, boosts))
          case com.patson.model.AirportBoostType.VACATION_HUB =>
            newFeatures.append(VacationHubFeature(0, boosts))
          case com.patson.model.AirportBoostType.FINANCIAL_HUB =>
            newFeatures.append(FinancialHubFeature(0, boosts))
          case _ =>
        }
    }
    (baseFeatures ++ newFeatures).groupBy(_.getClass).map {
      case(clazz, features) =>
        if (features.size <= 1) {
          features(0)
        } else { //should be 2
          features(0) match {
            case basicFeature : InternationalHubFeature => InternationalHubFeature(basicFeature.baseStrength, features(1).asInstanceOf[InternationalHubFeature].boosts)
            case basicFeature : FinancialHubFeature => FinancialHubFeature(basicFeature.baseStrength, features(1).asInstanceOf[FinancialHubFeature].boosts)
            case basicFeature : VacationHubFeature => VacationHubFeature(basicFeature.baseStrength, features(1).asInstanceOf[VacationHubFeature].boosts)
            case _ => features(0) //don't know how to merge
          }
        }
    }.toList
  }
}

case class AirlineAppeal(loyalty : Double, awareness : Double)
object AirlineAppeal {
  val MAX_LOYALTY = 100
  val MAX_AWARENESS = 100
}
case class AirlineBonus(bonusType : BonusType.Value, bonus : AirlineAppeal, expirationCycle : Option[Int]) {
  val isExpired : Int => Boolean = currentCycle => expirationCycle.isDefined && currentCycle > expirationCycle.get
}

object BonusType extends Enumeration {
  type BonusType = Value
  val NATIONAL_AIRLINE, PARTNERED_AIRLINE, OLYMPICS_VOTE, OLYMPICS_PASSENGER, SANTA_CLAUS, CAMPAIGN, NEGOTIATION_BONUS, BASE_SPECIALIZATION_BONUS, NO_BONUS = Value
  val description : BonusType.Value => String = {
    case NATIONAL_AIRLINE => "National Airline"
    case PARTNERED_AIRLINE => "Partnered Airline"
    case OLYMPICS_VOTE => "Olympics Vote Reward"
    case OLYMPICS_PASSENGER => "Olympics Goal Reward"
    case SANTA_CLAUS => "Santa Claus Reward"
    case CAMPAIGN => "Campaign"
    case NEGOTIATION_BONUS => "Negotiation Great Success"
    case BASE_SPECIALIZATION_BONUS => "Base Specialization Bonus"
    case NO_BONUS => "N/A"

  }
}

object Airport {
  def fromId(id : Int) = {
    val airportWithJustId = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
    airportWithJustId.id = id
    airportWithJustId
  }
  
  val MAJOR_AIRPORT_LOWER_THRESHOLD = 5
  val HQ_GUARANTEED_SLOTS = 20 //at least 20 slots for HQ
  val BASE_GUARANTEED_SLOTS = 10 //at least 10 slots for base
  val NON_BASE_MAX_SLOT = 70
  val MIN_RUNWAY_LENGTH = 750
  
  import FlightType._
  val qualityExpectationFlightTypeAdjust = 
  Map(SHORT_HAUL_DOMESTIC -> LinkClassValues.getInstance(-15, -5, 5),
        SHORT_HAUL_INTERNATIONAL ->  LinkClassValues.getInstance(-10, 0, 10),
        SHORT_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(-5, 5, 15),
        MEDIUM_HAUL_DOMESTIC -> LinkClassValues.getInstance(-5, 5, 15),
        MEDIUM_HAUL_INTERNATIONAL ->  LinkClassValues.getInstance(0, 5, 15),
        MEDIUM_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(0, 5, 15),
        LONG_HAUL_DOMESTIC -> LinkClassValues.getInstance(0, 5, 15),
        LONG_HAUL_INTERNATIONAL -> LinkClassValues.getInstance(5, 10, 20),
        LONG_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(10, 15, 20),
        ULTRA_LONG_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(10, 15, 20))
}

case class Runway(length : Int, code : String, runwayType : RunwayType.Value, lighted : Boolean)

object RunwayType extends Enumeration {
    type RunwayType = Value
    val Asphalt, Concrete, Gravel, Unknown = Value
}


