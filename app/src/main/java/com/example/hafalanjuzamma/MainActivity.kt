package com.example.hafalanjuzamma

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hafalanjuzamma.ui.theme.HafalanJuzAmmaTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import kotlin.collections.forEach
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity
data class Hafalan(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val nama: String,
  val kelas: String,
  val surat: String,
  val nilai: String
)

@Dao
interface HafalanDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(hafalan: Hafalan)

  @Delete
  suspend fun delete(hafalan: Hafalan)

  @Query("SELECT * FROM Hafalan")
  fun getAll(): Flow<List<Hafalan>>
}

@Database(entities = [Hafalan::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun hafalanDao(): HafalanDao
}

class HafalanViewModel(private val dao: HafalanDao) : ViewModel() {
  val hafalanList = dao.getAll()

  suspend fun insert(h: Hafalan) = dao.insert(h)
  suspend fun delete(h: Hafalan) = dao.delete(h)
}

class HafalanViewModelFactory(private val dao: HafalanDao) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return HafalanViewModel(dao) as T
  }
}

object DatabaseProvider {
  @Volatile
  private var INSTANCE: AppDatabase? = null

  fun getDatabase(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
      val instance = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "hafalan-db"
      ).build()
      INSTANCE = instance
      instance
    }
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val db = DatabaseProvider.getDatabase(applicationContext)

    val vm = ViewModelProvider(this, HafalanViewModelFactory(db.hafalanDao()))
      .get(HafalanViewModel::class.java)

    enableEdgeToEdge()
    setContent {
      HafalanJuzAmmaTheme {
        AppScreen(vm)
      }
    }
  }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppScreen(vm: HafalanViewModel) {
  val list by vm.hafalanList.collectAsState(initial = emptyList())
  var showForm by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  if (showForm) {
    FormAdd(
      onSave = {
        coroutineScope.launch { vm.insert(it) }
        showForm = false
      },
      onCancel = { showForm = false }
    )
  } else {
    Scaffold(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
          Spacer(modifier = Modifier.weight(1f))
          Button(onClick = { showForm = true }) {
            Text("Tambah Data")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HeaderRow()
        HorizontalDivider()

        LazyColumn {
          items(list) { item ->
            DataRow(item, onDelete = {
              coroutineScope.launch { vm.delete(item) }
            })
            HorizontalDivider()
          }
        }
      }
    }
  }
}

@Composable
fun HeaderRow() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Gray)
  ) {
    listOf("Nama", "Kelas", "Surat", "Nilai", "Aksi").forEach {
      Text(
        it,
        modifier = Modifier
          .weight(1f)
          .padding(8.dp),
        fontWeight = FontWeight.Bold,
        color = Color.Black
      )
    }
  }
}

@Composable
fun DataRow(item: Hafalan, onDelete: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth()) {
    listOf(item.nama, item.kelas, item.surat, item.nilai).forEach {
      Text(
        it,
        modifier = Modifier
          .weight(1f)
          .padding(8.dp)
      )
    }
    Button(
      onClick = onDelete,
      modifier = Modifier.padding(4.dp)
    ) {
      Text("Hapus")
    }
  }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FormAdd(onSave: (Hafalan) -> Unit, onCancel: () -> Unit) {
  var nama by remember { mutableStateOf("") }
  var kelas by remember { mutableStateOf("") }
  var surat by remember { mutableStateOf("") }
  var nilai by remember { mutableStateOf("") }

  Scaffold {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Form Tambah Data", fontWeight = FontWeight.Bold, fontSize = 20.sp)

      OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama") })
      OutlinedTextField(value = kelas, onValueChange = { kelas = it }, label = { Text("Kelas") })
      OutlinedTextField(value = surat, onValueChange = { surat = it }, label = { Text("Surat") })
      OutlinedTextField(value = nilai, onValueChange = { nilai = it }, label = { Text("Nilai") })

      Spacer(modifier = Modifier.height(16.dp))

      Row {
        Button(onClick = {
          onSave(
            Hafalan(
              nama = nama,
              kelas = kelas,
              surat = surat,
              nilai = nilai
            )
          )
        }) {
          Text("Simpan")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onCancel) {
          Text("Batal")
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TablePreview() {
  HafalanJuzAmmaTheme {
    Scaffold { innerPadding ->
      Column(modifier = Modifier.padding(innerPadding)) {
        HeaderRow()
        DataRow(item = Hafalan(nama = "Ali", kelas = "5A", surat = "An-Nas", nilai = "90")) {}
      }
    }
  }
}
