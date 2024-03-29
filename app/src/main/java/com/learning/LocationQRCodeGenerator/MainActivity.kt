package com.learning.LocationQRCodeGenerator


import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.learning.LocationQRCodeGenerator.ui.theme.QRCodeGeradorTheme
import com.learning.qrcodegerador.R

class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    //Google Play Services APIlerinin degiskenler
    //Konum hizmetleine erismek icin
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    //Konum bilgilerini guncellemek icin
    private lateinit var locationCallback: LocationCallback
    //ilk basta guncellemey ihtiyac olmadigini anlatir
    private var locationRequired: Boolean = false

    override fun onResume() {
        super.onResume()
        if(locationRequired){
            startLocationUpdates()
        }
    }
    override fun onPause() {
        super.onPause()
        locationCallback.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()


            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {

            var currentLocation by remember {
                mutableStateOf(LatLng(0.toDouble(),0.toDouble()))
            }

            locationCallback = object: LocationCallback(){
                override fun onLocationResult(p0: LocationResult) {
                    //degisklik oldugunda surekli guncellenir
                    super.onLocationResult(p0)
                    for (location in p0.locations){
                        currentLocation = LatLng(location.latitude,location.longitude)
                    }
                }
            }

            QRCodeGeradorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFCCC2DC)
                ) {
                    QRCodeApp(this@MainActivity, currentLocation)
                }
            }
        }
    }

    @Composable
//    @OptIn(ExperimentalMaterial3Api::class)
    fun QRCodeApp(context: Context, currentLocation: LatLng) {

        var qrCodeGenerated by remember { mutableStateOf<Bitmap?>(null) }
        var buttonText = "Create the QR Code"

        val launchMultiplePermissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
                permissionMaps ->
            val areGranted = permissionMaps.values.reduce {acc, next ->acc && next}
            if(areGranted) {
                locationRequired  = true
                startLocationUpdates()
                Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        Column(
            modifier = Modifier.background(Color(0xFFCCC2DC)).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,

        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Location QR Code Generator",
                    textAlign = TextAlign.Center,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                )

                if (qrCodeGenerated != null) {
                    buttonText = "Refresh the location"
                    Image(bitmap = qrCodeGenerated!!.asImageBitmap(),
                        contentDescription = "the location",
                        modifier = Modifier.size(300.dp))
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_qr_code_scanner_24),
                        contentDescription = "",
                        modifier = Modifier.size(180.dp)
                    )
                }
            }

            Column {
                Button(onClick = {
                    if(permissions.all{
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        })
                    {
                        //Get Location
                        startLocationUpdates()
                        if(currentLocation.latitude != 0.0){
                            qrCodeGenerated = generateQrCode("https://www.google.com/maps/search/?api=1&query=${currentLocation.latitude},${currentLocation.longitude}")
                        }
                    }
                    else{
                        launchMultiplePermissions.launch(permissions)
                    }

                }) {
                    Text(text = buttonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
//                        color = Color.Black
                    )
                }


            }
        }
    }

    private fun generateQrCode(text: String) : Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val w = matrix.width
        val h = matrix.height

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)

        for (x in 0 until h) {
            for (y in 0 until w) {
                bitmap.setPixel(x, y, if (matrix.get(x, y))
                    android.graphics.Color.BLACK
                else
                    android.graphics.Color.rgb(204,194,220)
                )
            }
        }

        return bitmap
    }
}