package com.example.sih.studentHistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.sih.R
import com.example.sih.database.ScoreDatabase
import com.example.sih.database.StudentData
import com.example.sih.database.StudentDatabase
import com.example.sih.databinding.FragmentStudentHistoryBinding
import com.github.mikephil.charting.charts.LineChart

class StudentHistoryFragment : Fragment(){

    private lateinit var binding : FragmentStudentHistoryBinding
    private lateinit var viewModel: StudentHistoryViewModel
    private lateinit var viewModelFactory : StudentHistoryViewModelFactory
    private lateinit var adapter: StudentHistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_student_history,container,false
        )
        val application = requireNotNull(this.activity).application
        val dataSource = StudentDatabase.getInstance(application).studentDatabaseDao
        val name = StudentHistoryFragmentArgs.fromBundle(requireArguments()).student
        viewModelFactory = StudentHistoryViewModelFactory(dataSource,application,name)
        viewModel = ViewModelProvider(this,viewModelFactory).get(StudentHistoryViewModel::class.java)
        binding.lifecycleOwner=this

        adapter = StudentHistAdapter()
        binding.studentHistList.adapter=adapter

        viewModel.session.observe(viewLifecycleOwner, Observer {
            if(it!=null){
                adapter.submitList(it)
                adapter.notifyDataSetChanged()
            }
        })

        return binding.root
    }

}