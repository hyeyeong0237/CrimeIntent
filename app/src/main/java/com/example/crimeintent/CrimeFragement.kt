package com.example.crimeintent


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*


private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "yyyy년 M월 d일 H시 m분, E요일"


class CrimeFragement : Fragment(), DatePickerFragment.Callbacks{


    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri : Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton : Button
    private lateinit var solvedCheckBox : CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView : ImageView
    private lateinit var saveButton: Button
    private lateinit var RadioGroup :RadioGroup

    private val crimeDetailViewModel :CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragement_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        saveButton = view.findViewById(R.id.save_button) as Button
        RadioGroup = view.findViewById(R.id.radioGroup) as RadioGroup


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let{
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(), "com.example.crimeintent.fileprovider", photoFile)
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragement, REQUEST_DATE)
                show(this@CrimeFragement.getParentFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
                putExtra(Intent.EXTRA_STREAM, photoUri)
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply{
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null){
                isEnabled = false
            }

        }

        photoButton.apply {
            val packageManager : PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity : ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null){
                isEnabled =false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities : List<ResolveInfo> = packageManager.queryIntentActivities(captureImage,PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName, photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }


        saveButton.apply {

            setOnClickListener {
                crimeDetailViewModel.saveCrime(crime)
                Toast.makeText(getContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()

            }

        }
        
        RadioGroup.apply{
            setOnCheckedChangeListener { group, checkedId ->
                if(checkedId == R.id.radioButton1 )
                    Toast.makeText(getContext(), "Happy가 눌려졌습니다",Toast.LENGTH_SHORT).show()
                if(checkedId == R.id.radioButton2)
                    Toast.makeText(getContext(), "Sad가 눌려졌습니다",Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onStop() {
        super.onStop()
        //crimeDetailViewModel.saveCrime(crime)
        /**
         * crimeDetailViewModel.deleteCrime(crime)
         */
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if(crime.suspect.isNotEmpty()){
            suspectButton.text = crime.suspect
        }

        updatePhotoView()
    }
    private fun updatePhotoView(){
        val matrix = Matrix()
        matrix.postRotate(90F)

        if(photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
            photoView.setImageBitmap(rotatedBitmap)
        }else{
            photoView.setImageDrawable(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when{
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri : Uri = data.data?: return
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor = requireActivity().contentResolver.query(contactUri,queryFields,null, null, null)
                cursor?.use{
                    if(it.count == 0){
                        return
                    }
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    //crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun getCrimeReport() : String{
        val solvedString = if (crime.isSolved){
            getString(R.string.crime_report_solved)
        }else{
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT,crime.date).toString()
        val suspect = if (crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        }else{
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    companion object {

        fun newInstance(crimeId: UUID) :CrimeFragement {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragement().apply {
                arguments = args
            }
        }
    }


}