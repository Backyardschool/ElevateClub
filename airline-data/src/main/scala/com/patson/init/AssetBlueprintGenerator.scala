package com.patson.init

import com.patson.data.AirportAssetSource
import com.patson.model.{AirportAssetType, _}

import scala.collection.mutable.ListBuffer
import scala.util.Random


object AssetBlueprintGenerator {
  val assetTypesByGroup : Map[GenerationGroup.Value, AirportAssetType.ValueSet] = AirportAssetType.values.groupBy(GenerationGroup.getGenerationGroup(_))
  val random = new Random(1234567890123L)

  def main(airports : List[Airport]) : Unit = {
    AirportAssetSource.deleteAllAirportAssetBlueprints()

    airports.foreach { airport =>
      //generate unique first
      val generatedAssetTypes = ListBuffer[AirportAssetType.Value]()
      generateUniqueBlueprints(airport, generatedAssetTypes)
      generateWeatherBlueprints(airport, generatedAssetTypes)
      generateGeneralBlueprints(airport, generatedAssetTypes)

      println(s"$airport => $generatedAssetTypes")

      val blueprints = generatedAssetTypes.map(assetType => AirportAssetBlueprint(airport, assetType)).toList
      AirportAssetSource.saveAirportAssetBlueprints(blueprints)
    }
  }



  def generateUniqueBlueprints(airport : Airport, generatedBlueprints : ListBuffer[AirportAssetType.Value]) = {
    val maxCount = if (airport.size <= 6) 1 else airport.size - 5
    generateBlueprints(airport, 1, maxCount, assetTypesByGroup(GenerationGroup.UNIQUE), generatedBlueprints)
  }

  def generateWeatherBlueprints(airport : Airport, generatedBlueprints : ListBuffer[AirportAssetType.Value]) = {
    generateBlueprints(airport, 1, 1, assetTypesByGroup(GenerationGroup.WEATHER), generatedBlueprints)
  }

  def generateGeneralBlueprints(airport : Airport, generatedBlueprints : ListBuffer[AirportAssetType.Value]) = {
    generateBlueprints(airport, 5, airport.size, assetTypesByGroup(GenerationGroup.GENERAL), generatedBlueprints)
  }

  def generateBlueprints(airport : Airport, iterationCount : Int, maxCount : Int, candidates : AirportAssetType.ValueSet, generatedBlueprints : ListBuffer[AirportAssetType.Value]) : Unit =  {
    val limit = Math.min(getAirportBlueprintsLimit(airport) - generatedBlueprints.size, maxCount)
    val assetTypes = candidates.toList.sortBy(_.id).reverse //consider the bigger enum ID first (lower items have higher precedence within the group)
    val newAssetTypes = ListBuffer[AirportAssetType.Value]()
    for (i <- 0 until iterationCount) {
      assetTypes.foreach { assetType =>
        if (newAssetTypes.length >= limit) {
          generatedBlueprints.addAll(newAssetTypes)
          return
        }
        if (isApplicable(assetType, airport)) {
          val odds = generationOdds(assetType, airport)
          if (odds >= random.nextDouble()) { //picked!
            newAssetTypes.append(assetType)
          }
        }
      }
    }

    generatedBlueprints.addAll(newAssetTypes)
  }

  def generationOdds(assetType: AirportAssetType.Value, airport : Airport) : Double = {
    import AirportAssetType._
    assetType match {
      case GRAND_HOTEL_TOURIST =>
        0.5
      case GRAND_HOTEL_BUSINESS =>
        0.5
      case AMUSEMENT_PARK =>
        0.2
      case SUBWAY =>
        0.8
      case STADIUM =>
        0.5
      case SCIENCE_PARK =>
        1
      case CONVENTION_CENTER =>
        0.5
      case MUSEUM =>
        1
      case LANDMARK =>
        0.5
      case SOLAR_POWER_PLANT =>
        if (airport.size <= 4) 0.1 else 0.2 * Math.min(airport.size - 4, 3)
      case BEACH_RESORT =>
        if (airport.features.map(_.featureType).contains(AirportFeatureType.VACATION_HUB)) {
          1
        } else {
          if (airport.size <= 4) 0.3 else if (airport.size == 5) 0.5 else if (airport.size == 6) 0.7 else 1
        }
      case SKI_RESORT =>
        if (airport.size <= 4) 0.3 else if (airport.size == 5) 0.5 else if (airport.size == 6) 0.7 else 1
      case TRAVEL_AGENCY =>
        0.5
      case SPORT_ARENA =>
        0.5
      case GAME_ARCADE =>
        0.2
      case CINEMA =>
        0.2
      case INN =>
        0.3
      case GOLF_COURSE =>
        0.25
      case OFFICE_BUILDING_1 =>
        0.3
      case HOTEL =>
        0.3
      case OFFICE_BUILDING_2 =>
        0.3
      case RESTAURANT =>
        0.2
      case OFFICE_BUILDING_3 =>
        if (airport.incomeLevel < 40) 0.01 * airport.incomeLevel else 0.7
      case SHOPPING_MALL =>
        0.3
      case LUXURIOUS_HOTEL =>
        0.3
      case OFFICE_BUILDING_4 =>
        if (airport.incomeLevel < 45) 0.01 * airport.incomeLevel else 1
      case CITY_TRANSIT =>
        0.7
      case AIRPORT_HOTEL =>
        1
      case RESIDENTIAL_COMPLEX =>
        0.5
    }
  }

  val getAirportBlueprintsLimit = (airport : Airport) => airport.size

  val scienceParkIatas = List("SFO", "SEA", "SZX", "SIN", "LHR", "ICN", "TPE", "AMS", "BER", "BOS")

  import AirportAssetType._
  val isApplicable = (assetType : AirportAssetType.Value, airport : Airport) => assetType match {
    case GRAND_HOTEL_TOURIST =>
      airport.getFeatures().find(_.featureType == AirportFeatureType.VACATION_HUB).isDefined
    case GRAND_HOTEL_BUSINESS =>
      airport.getFeatures().find(_.featureType == AirportFeatureType.FINANCIAL_HUB).isDefined
    case AMUSEMENT_PARK =>
      airport.basePopulation >= 500000 && airport.size >= 5
    case SUBWAY =>
      airport.basePopulation >= 2000000 && airport.size >= 6
    case STADIUM =>
      airport.basePopulation >= 1000000 && airport.size >= 5
    case SCIENCE_PARK =>
      scienceParkIatas.contains(airport.iata)
    case CONVENTION_CENTER =>
      airport.basePopulation >= 2000000 && airport.size >= 6
    case MUSEUM =>
      airport.basePopulation >= 1000000 && airport.size >= 6
    case LANDMARK =>
      airport.basePopulation >= 3000000 && airport.size >= 7 && airport.getFeatures().find { feature =>
        feature.featureType == AirportFeatureType.FINANCIAL_HUB ||
        feature.featureType == AirportFeatureType.VACATION_HUB ||
        feature.featureType == AirportFeatureType.INTERNATIONAL_HUB
      }.isDefined
    case SOLAR_POWER_PLANT =>
      AirportWeatherData.getAirportWeatherData(airport) match {
        case Some(data) => data.sunHourPerDay >= 12
        case None => false
      }
    case BEACH_RESORT =>
      AirportWeatherData.getAirportWeatherData(airport) match {
        case Some(data) => data.minTemperature >= 20 && data.maxTemperature <= 30
        case None => false
      }
    case SKI_RESORT =>
      AirportWeatherData.getAirportWeatherData(airport) match {
        case Some(data) => data.snowPerDay >= 0.4
        case None => false
      }
    case TRAVEL_AGENCY =>
      airport.basePopulation >= 300000 && airport.incomeLevel >= 25
    case SPORT_ARENA =>
      airport.basePopulation >= 500000 && airport.incomeLevel >= 15
    case GAME_ARCADE =>
      airport.basePopulation >= 100000 && airport.incomeLevel >= 20
    case CINEMA =>
      airport.basePopulation >= 100000 && airport.incomeLevel >= 20
    case INN =>
      airport.basePopulation >= 10000
    case GOLF_COURSE =>
      airport.basePopulation >= 100000 && airport.incomeLevel >= 40 && airport.size <= 5
    case OFFICE_BUILDING_1 =>
      airport.basePopulation >= 500000
    case HOTEL =>
      airport.basePopulation >= 500000 && airport.incomeLevel >= 25
    case OFFICE_BUILDING_2 =>
      airport.basePopulation >= 1000000
    case RESTAURANT =>
      airport.incomeLevel >= 20
    case OFFICE_BUILDING_3 =>
      airport.basePopulation >= 3000000 && airport.incomeLevel >= 30
    case SHOPPING_MALL =>
      airport.basePopulation >= 1000000 && airport.incomeLevel >= 25
    case LUXURIOUS_HOTEL =>
      airport.basePopulation >= 500000 && airport.incomeLevel >= 45
    case OFFICE_BUILDING_4 =>
      airport.basePopulation >= 6000000 && airport.incomeLevel >= 35
    case CITY_TRANSIT =>
      airport.basePopulation >= 1000000 && airport.incomeLevel >= 10
    case AIRPORT_HOTEL =>
      airport.basePopulation >= 100000 && airport.size >= 4
    case RESIDENTIAL_COMPLEX =>
      airport.basePopulation < 300000

  }
}

object GenerationGroup extends Enumeration {
  type GenerationGroup = Value
  val UNIQUE, WEATHER, GENERAL = Value

  import AirportAssetType._
  val getGenerationGroup = (assetType : AirportAssetType.Value) => assetType match {
    case GRAND_HOTEL_TOURIST => UNIQUE
    case GRAND_HOTEL_BUSINESS => UNIQUE
    case AMUSEMENT_PARK => UNIQUE
    case SUBWAY => UNIQUE
    case STADIUM => UNIQUE
    case SCIENCE_PARK => UNIQUE
    case CONVENTION_CENTER => UNIQUE
    case MUSEUM => UNIQUE
    case LANDMARK => UNIQUE

    case SOLAR_POWER_PLANT => WEATHER
    case BEACH_RESORT => WEATHER
    case SKI_RESORT => WEATHER

    case TRAVEL_AGENCY => GENERAL
    case SPORT_ARENA => GENERAL
    case GAME_ARCADE => GENERAL
    case CINEMA => GENERAL
    case INN => GENERAL
    case GOLF_COURSE => GENERAL
    case OFFICE_BUILDING_1 => GENERAL
    case HOTEL => GENERAL
    case OFFICE_BUILDING_2 => GENERAL
    case RESTAURANT => GENERAL
    case OFFICE_BUILDING_3 => GENERAL
    case SHOPPING_MALL => GENERAL
    case LUXURIOUS_HOTEL => GENERAL
    case OFFICE_BUILDING_4 => GENERAL
    case CITY_TRANSIT => GENERAL
    case AIRPORT_HOTEL => GENERAL
    case RESIDENTIAL_COMPLEX => GENERAL
  }
}


