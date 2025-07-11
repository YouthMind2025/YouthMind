import SwiftUI

struct CreatePostView: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var title: String = ""
    @State private var content: String = ""
    @State private var showAlert = false
    @State private var alertMessage = ""
    @State private var isSuccess = false
    
    var titleValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    var contentValid: Bool {
        !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    var canSubmit: Bool {
        titleValid && contentValid && !viewModel.isLoading
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Title input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Title")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    TextField("Enter post title", text: $title)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .autocapitalization(.sentences)
                    
                    if !titleValid && !title.isEmpty {
                        Text("Title cannot be empty")
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
                
                // Content input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    TextEditor(text: $content)
                        .frame(minHeight: 120)
                        .padding(8)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color(.systemGray4), lineWidth: 1)
                        )
                    
                    if !contentValid && !content.isEmpty {
                        Text("Content cannot be empty")
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
                
                // Submit button
                Button(action: {
                    createPost()
                }) {
                    HStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .scaleEffect(0.8)
                                .foregroundColor(.white)
                        }
                        Text(viewModel.isLoading ? "Creating..." : "Create Post")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(canSubmit ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(!canSubmit)
                
                Spacer()
            }
            .padding()
            .navigationTitle("Create Post")
            .navigationBarTitleDisplayMode(.large)
        }
        .onReceive(viewModel.$errorMessage) { errorMessage in
            if let error = errorMessage {
                alertMessage = error
                isSuccess = false
                showAlert = true
            }
        }
        .onReceive(viewModel.$successMessage) { successMessage in
            if let success = successMessage {
                alertMessage = success
                isSuccess = true
                showAlert = true
                // 清空表单
                title = ""
                content = ""
            }
        }
        .overlay(
            // Retry alert
            Group {
                if viewModel.showRetryAlert {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .onTapGesture {
                            viewModel.showRetryAlert = false
                        }
                    
                    RetryAlertView(
                        message: viewModel.errorMessage ?? "Network error",
                        retryAction: {
                            viewModel.retryLastAction()
                        },
                        dismissAction: {
                            viewModel.showRetryAlert = false
                        }
                    )
                }
            }
        )
        .alert(isSuccess ? "Success" : "Error", isPresented: $showAlert) {
            Button("OK") {
                showAlert = false
            }
        } message: {
            Text(alertMessage)
        }
    }
    
    private func createPost() {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        viewModel.createPost(title: trimmedTitle, content: trimmedContent)
    }
}

struct CreatePostView_Previews: PreviewProvider {
    static var previews: some View {
        CreatePostView()
    }
} 