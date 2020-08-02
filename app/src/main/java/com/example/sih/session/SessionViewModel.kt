package com.example.sih.session

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sih.database.MyConverters
import com.example.sih.database.ScoreDatabaseDao
import com.example.sih.database.StudentScore
import com.github.mikephil.charting.data.BarDataSet
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*

class SessionViewModel(private val database: ScoreDatabaseDao,
                       application: Application

): AndroidViewModel(application){

    private var myConverters=MyConverters()
    // this counter will hold category count of each student
    var counterCollection = HashMap<String, Counter>()
    // this list will store all the mean_scores

    private lateinit var set : BarDataSet
    private val fireBaseDatabase = Firebase.database
    private var teacherRef = fireBaseDatabase.reference.child("nameList").child("teacher_name")
    private var statusRef =  fireBaseDatabase.reference.child("online")
    private var viewModelJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var  sessionId : String? = null
    private var scores = mutableListOf<Double>()

    private val currScores = StudentScore("","")

    private val _drowsy = MutableLiveData<MutableList<Student?>>()
    val drowsy : LiveData<MutableList<Student?>>
        get() = _drowsy

    private val _inattentive = MutableLiveData<MutableList<Student?>>()
    val inattentive : LiveData<MutableList<Student?>>
        get() = _inattentive

    private val _attentive = MutableLiveData<MutableList<Student?>>()
    val attentive : LiveData<MutableList<Student?>>
        get() = _attentive

    private val _interactive = MutableLiveData<MutableList<Student?>>()
    val interactive : LiveData<MutableList<Student?>>
        get() = _interactive


    private fun clearDatabase(){
        uiScope.launch {
            withContext(Dispatchers.IO){
                database.clear()
            }
        }
    }

    init {
        _drowsy.value= mutableListOf()
        _inattentive.value= mutableListOf()
        _attentive.value= mutableListOf()
        _interactive.value= mutableListOf()
    }


    fun saveHistory(){
        Log.d("History","History being saved")
        uiScope.launch {
            withContext(Dispatchers.IO){
                currScores.id= sessionId as String
                currScores.studentScore=myConverters.listToString(scores)
                database.insert(currScores)
            }
        }
    }

    fun readFireBase(){

        uiScope.launch {
            withContext(Dispatchers.IO){

                statusRef.addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Failed to read value", error.toException())
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val v  = snapshot.getValue<String>()
                        if (v != null) {
                            sessionId = v
                            makeData()
                        }
                        else{
                            saveHistory()
                        }
                    }

                })
            }
        }
    }


    fun makeData(){

        //val scoreRef = fireBaseDatabase.reference.child("means_score")
        val myRef = fireBaseDatabase.reference.child("Teacher")

        val childEventListener = object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value", error.toException())
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                val value = key?.let { Student(it) }
                var drowsyList = _drowsy.value
                drowsyList?.remove(value)
                var inattentiveList = _inattentive.value
                inattentiveList?.remove(value)
                var attentivelist = _attentive.value
                attentivelist?.remove(value)
                var interactiveList = _interactive.value
                interactiveList?.remove(value)

                when(snapshot.getValue<Long>()){
                    0L -> {
                        counterCollection[key.toString()]?.currentState = 0
                        drowsyList?.add(value)
                    }
                    1L -> {
                        counterCollection[key.toString()]?.currentState = 1
                        inattentiveList?.add(value)
                    }
                    2L -> {
                        counterCollection[key.toString()]?.currentState = 2
                        attentivelist?.add(value)
                    }
                    else -> {
                        counterCollection[key.toString()]?.currentState = 3
                        interactiveList?.add(value)
                    }
                }

                _drowsy.postValue(drowsyList)
                _inattentive.postValue(inattentiveList)
                _attentive.postValue(attentivelist)
                _inattentive.postValue(interactiveList)
                val a = counterCollection["Surbhi"]?.cArr?.contentToString()
                Log.d(TAG, "mean_counter $a")

            }


            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                counterCollection[key.toString()] = Counter()
                val value = key?.let { Student(it) }
                Log.d("Category",value.toString()+"-"+snapshot.value.toString())
                when(snapshot.getValue<Long>()){
                    0L -> {
                        counterCollection[key.toString()]!!.currentState = 0
                        val temp = _drowsy.value
                        temp?.add(value)
                        _drowsy.postValue(temp)
                    }
                    1L -> {

                        counterCollection[key.toString()]!!.currentState = 1
                        val temp = _inattentive.value
                        temp?.add(value)
                        _inattentive.postValue(temp)
                    }
                    2L -> {

                        counterCollection[key.toString()]!!.currentState = 1
                        val temp = _attentive.value
                        temp?.add(value)
                        _attentive.postValue(temp)
                    }
                    else -> {
                        counterCollection[key.toString()]!!.currentState = 1
                        val temp = _interactive.value
                        temp?.add(value)
                        _interactive.postValue(temp)
                    }
                }
                //Log.d(TAG, "Value is : $value, $k")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

        }

        myRef.addChildEventListener(childEventListener)

        /*
        scoreRef.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value", error.toException())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue<Long>()
                if (score != null) {
                    scores.add(score)
                }
                //Log.d(TAG, "Value is score : $score")
            }

        })*/

        meanScoreListener()
    }

    fun meanScoreListener(){
        val scoreRef = fireBaseDatabase.reference.child("means_score")
        scoreRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "mean-value Error $error")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue<Double>()
                if (value != null){
                    scores.add(value.toDouble())
                    for(key in counterCollection.keys){
                        val index = counterCollection[key]?.currentState
                        counterCollection[key]!!.cArr[index!!] += 1
                    }
                }
                Log.d(TAG,"mean_value $scores")
            }

        })
    }


    fun getPercen(name: String){

    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
