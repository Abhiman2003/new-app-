package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE dateString = :dateString ORDER BY startTime DESC")
    fun getTripsByDate(dateString: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY startTime ASC")
    fun getTripsInRange(startDate: String, endDate: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    fun getTripById(id: Long): Flow<TripEntity?>

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripEntity>)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()
}
