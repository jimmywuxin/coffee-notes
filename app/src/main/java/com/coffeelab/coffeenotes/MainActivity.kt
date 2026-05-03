package com.coffeelab.coffeenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.ui.navigation.CoffeeNavGraph
import com.coffeelab.coffeenotes.ui.theme.CoffeeNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure default equipment and grinders exist on every startup
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val equipmentDao = db.equipmentDao()
            val grinderDao = db.grinderDao()
            if (equipmentDao.getAllOnce().isEmpty()) {
                val items = Equipment.DEFAULT_EQUIPMENT.mapIndexed { index, name ->
                    Equipment(name = name, sortOrder = index)
                }
                equipmentDao.insertAll(items)
            }
            if (grinderDao.getAllOnce().isEmpty()) {
                val items = Grinder.DEFAULT_GRINDERS.mapIndexed { index, name ->
                    Grinder(name = name, sortOrder = index)
                }
                grinderDao.insertAll(items)
            }
        }

        setContent {
            CoffeeNotesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    CoffeeNavGraph(navController = navController)
                }
            }
        }
    }
}
