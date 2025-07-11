import SwiftUI

struct APITestView: View {
    @StateObject private var homeViewModel = HomeViewModel()
    @StateObject private var loginViewModel = LoginViewModel()
    
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var otp: String = ""
    @State private var postTitle: String = ""
    @State private var postContent: String = ""
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Token status display
                    TokenStatusView()
                    
                    // Registration/Login section
                    GroupBox("Authentication") {
                        VStack(spacing: 15) {
                            TextField("Email", text: $email)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                            
                            SecureField("Password", text: $password)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                            
                            TextField("OTP (6 digits)", text: $otp)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .keyboardType(.numberPad)
                            
                            HStack(spacing: 10) {
                                Button("Register") {
                                    loginViewModel.email = email
                                    loginViewModel.otp = otp
                                    loginViewModel.register()
                                }
                                .buttonStyle(.borderedProminent)
                                .disabled(email.isEmpty || otp.isEmpty || loginViewModel.isLoading)
                                
                                Button("Login") {
                                    homeViewModel.login(email: email, password: password)
                                }
                                .buttonStyle(.borderedProminent)
                                .disabled(email.isEmpty || password.isEmpty || homeViewModel.isLoading)
                            }
                        }
                        .padding()
                    }
                    
                    // Create post section
                    GroupBox("Create Post") {
                        VStack(spacing: 15) {
                            TextField("Post title", text: $postTitle)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                            
                            TextEditor(text: $postContent)
                                .frame(height: 100)
                                .padding(8)
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color(.systemGray4), lineWidth: 1)
                                )
                            
                            Button("Create Post") {
                                homeViewModel.createPost(title: postTitle, content: postContent)
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(postTitle.isEmpty || postContent.isEmpty || homeViewModel.isLoading)
                        }
                        .padding()
                    }
                    
                    // Fetch posts section
                    GroupBox("Fetch Posts") {
                        VStack(spacing: 15) {
                            Button("Fetch Posts List") {
                                homeViewModel.fetchPosts()
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(homeViewModel.isLoading)
                            
                            if !homeViewModel.posts.isEmpty {
                                Text("Posts count: \(homeViewModel.posts.count)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding()
                    }
                    
                    // Status display
                    StatusDisplayView(
                        homeViewModel: homeViewModel,
                        loginViewModel: loginViewModel
                    )
                }
                .padding()
            }
            .navigationTitle("API Test")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

struct TokenStatusView: View {
    @State private var tokenStatus: String = "检查中..."
    
    var body: some View {
        GroupBox("Token Status") {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Token:")
                    Spacer()
                    Text(tokenStatus)
                        .foregroundColor(tokenStatus.contains("Valid") ? .green : .red)
                }
                
                Button("Refresh Status") {
                    updateTokenStatus()
                }
                .buttonStyle(.bordered)
            }
            .padding()
        }
        .onAppear {
            updateTokenStatus()
        }
    }
    
    private func updateTokenStatus() {
        if TokenManager.shared.isTokenValid() {
            tokenStatus = "Valid"
        } else {
            tokenStatus = "Invalid or not found"
        }
    }
}

struct StatusDisplayView: View {
    @ObservedObject var homeViewModel: HomeViewModel
    @ObservedObject var loginViewModel: LoginViewModel
    
    var body: some View {
        GroupBox("Status Information") {
            VStack(alignment: .leading, spacing: 10) {
                if homeViewModel.isLoading || loginViewModel.isLoading {
                    HStack {
                        ProgressView()
                            .scaleEffect(0.8)
                        Text("Loading...")
                    }
                }
                
                if let error = homeViewModel.errorMessage {
                    Text("Error: \(error)")
                        .foregroundColor(.red)
                        .font(.caption)
                }
                
                if let error = loginViewModel.errorMessage {
                    Text("Login Error: \(error)")
                        .foregroundColor(.red)
                        .font(.caption)
                }
                
                if let success = homeViewModel.successMessage {
                    Text("Success: \(success)")
                        .foregroundColor(.green)
                        .font(.caption)
                }
                
                if let success = loginViewModel.successMessage {
                    Text("Login Success: \(success)")
                        .foregroundColor(.green)
                        .font(.caption)
                }
                
                if loginViewModel.isLoggedIn {
                    Text("Login Status: Logged In")
                        .foregroundColor(.green)
                        .font(.caption)
                }
            }
            .padding()
        }
    }
}

struct APITestView_Previews: PreviewProvider {
    static var previews: some View {
        APITestView()
    }
} 