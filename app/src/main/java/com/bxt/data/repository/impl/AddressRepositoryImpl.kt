//package com.bxt.data.repository.impl
//
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.libraries.places.api.model.AutocompleteSessionToken
//import com.google.android.libraries.places.api.model.Place
//import com.google.android.libraries.places.api.net.FetchPlaceRequest
//import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
//import com.google.android.libraries.places.api.net.PlacesClient
//import kotlinx.coroutines.tasks.await
//import javax.inject.Inject
//import javax.inject.Singleton
//
//data class AddressSuggestion(
//    val placeId: String,
//    val primaryText: String,
//    val secondaryText: String
//)
//
//interface AddressRepository {
//    suspend fun searchAddresses(query: String): List<AddressSuggestion>
//    suspend fun getPlaceDetails(placeId: String): LatLng?
//}
//
//@Singleton
//class AddressRepositoryImpl @Inject constructor(
//    private val placesClient: PlacesClient
//) : AddressRepository {
//
//    private var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
//
//    override suspend fun searchAddresses(query: String): List<AddressSuggestion> {
//        val request = FindAutocompletePredictionsRequest.builder()
//            .setSessionToken(token)
//            .setQuery(query)
//            .setCountry("VN")
//            .build()
//        return try {
//            val response = placesClient.findAutocompletePredictions(request).await()
//            response.autocompletePredictions.map {
//                AddressSuggestion(
//                    placeId = it.placeId,
//                    primaryText = it.getPrimaryText(null).toString(),
//                    secondaryText = it.getSecondaryText(null).toString()
//                )
//            }
//        } catch (e: Exception) {
//            emptyList()
//        }
//    }
//
//    override suspend fun getPlaceDetails(placeId: String): LatLng? {
//        val request = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.LAT_LNG))
//        return try {
//            val response = placesClient.fetchPlace(request).await()
//            token = AutocompleteSessionToken.newInstance() // Reset token
//            response.place.latLng
//        } catch (e: Exception) {
//            null
//        }
//    }
//}