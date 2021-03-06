package com.example.sih.session

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sih.database.ScoreDatabaseDao
import com.example.sih.database.StudentDatabaseDao
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarDataSet
import java.lang.IllegalArgumentException

class SessionViewModelFactory (
    private val dataSource: ScoreDatabaseDao,
    private val application: Application,
    private val studentDatabaseDao: StudentDatabaseDao
) : ViewModelProvider.Factory{
    @Suppress("unchecked cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(SessionViewModel::class.java)){
            return SessionViewModel(dataSource,application,studentDatabaseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}