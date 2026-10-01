package com.example.gagebu

import androidx.room.*

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY id ASC")
    fun getAllAssets(): List<Asset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAsset(asset: Asset)

    @Update
    fun updateAsset(asset: Asset)

    @Delete
    fun deleteAsset(asset: Asset)
}