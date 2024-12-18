package com.example.sims

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var searchView: SearchView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper
    private var recyclerView: RecyclerView? = null
    private var recyclerViewUsersAdapter: RecyclerViewUsersAdapter? = null
    private var userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        firebaseHelper = FirebaseDatabaseHelper()

        header = findViewById(R.id.header)
        setupHeaderWithBackIcon()

        recyclerView = findViewById(R.id.rvViewUsers)
        recyclerViewUsersAdapter = RecyclerViewUsersAdapter(this@ManageUsersActivity, userList)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(this@ManageUsersActivity)
            adapter = recyclerViewUsersAdapter
        }

        setupSearchView()
        fetchUsersFromDatabase()

        findViewById<Button>(R.id.addUserBtn).setOnClickListener { showAddUserDialog() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupHeaderWithBackIcon() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val spannableString = SpannableString("  ${header.text}").apply {
            setSpan(ImageSpan(drawable!!, DynamicDrawableSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(DrawableClickSpan { onBackPressedDispatcher.onBackPressed() }, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchProduct)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    recyclerViewUsersAdapter?.resetList()
                } else {
                    recyclerViewUsersAdapter?.filter(newText)
                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        fetchUsersFromDatabase()
        searchView.setQuery("", false)
        searchView.clearFocus()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun fetchUsersFromDatabase() {
        firebaseHelper.fetchUsers { fetchedUsers ->
            userList.clear()
            val enabledUsers = fetchedUsers.filter { user -> user.enabled }
            userList.addAll(enabledUsers)
            recyclerViewUsersAdapter?.originalList = userList.toMutableList()
            recyclerViewUsersAdapter?.notifyDataSetChanged()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setTitle("Add User").create()

        val nameEditText = dialogView.findViewById<EditText>(R.id.uploadName)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.uploadUsername)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.uploadRole)

        val roles = arrayOf("Admin", "User")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleSpinner.adapter = adapter

        dialogView.findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val name = nameEditText.text.toString()
            val username = usernameEditText.text.toString()
            val selectedRole = roleSpinner.selectedItem.toString()
            val password = if (selectedRole == "Admin") "admin_password" else "user_password"

            if (name.isNotBlank() && username.isNotBlank()) {
                val isDuplicateUsername = userList.any { it.username == username }

                if (isDuplicateUsername) {
                    Toast.makeText(this, "Username already exists. Please choose a different username.", Toast.LENGTH_SHORT).show()
                } else {
                    addUserToDatabase(name, username, password, selectedRole)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addUserToDatabase(name: String, username: String, password: String, role: String) {
        firebaseHelper.addUser(username, password, name, role) { success ->
            Toast.makeText(
                this,
                if (success) "User added successfully!" else "Error adding user to database. Username may already exist.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}
