import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()
    @State private var showErrorBanner = false
    @State private var showSuccessBanner = false
    
    var emailValid: Bool {
        // Simple regex for email validation
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        let emailPred = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
        return emailPred.evaluate(with: viewModel.email)
    }
    var otpValid: Bool {
        viewModel.otp.count == 6 && viewModel.otp.allSatisfy { $0.isNumber }
    }
    var canSubmit: Bool {
        emailValid && otpValid && !viewModel.isLoading
    }
    var body: some View {
        VStack(spacing: 24) {
            Text("Login / Register")
                .font(.largeTitle)
                .bold()
                .padding(.top, 32)
            VStack(alignment: .leading, spacing: 12) {
                TextField("Email", text: $viewModel.email)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                if !emailValid && !viewModel.email.isEmpty {
                    Text("Please enter a valid email.")
                        .foregroundColor(.red)
                        .font(.caption)
                }
                SecureField("OTP", text: $viewModel.otp)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.numberPad)
                if !otpValid && !viewModel.otp.isEmpty {
                    Text("OTP must be 6 digits.")
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
            .padding(.horizontal)
            if viewModel.isLoading {
                ProgressView().padding(.vertical)
            }
            Button(action: {
                viewModel.register()
            }) {
                Text("Register / Login")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(canSubmit ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(!canSubmit)
            .padding(.horizontal)
            Button(action: {}) {
                Text("Resend OTP")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.secondary.opacity(0.2))
                    .foregroundColor(.gray)
                    .cornerRadius(10)
            }
            .disabled(true)
            .padding(.horizontal)
            if let error = viewModel.errorMessage {
                Text(error)
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.red)
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .transition(.move(edge: .top))
            }
            if let success = viewModel.successMessage {
                Text(success)
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .transition(.move(edge: .top))
            }
            if viewModel.isLoggedIn {
                Text("Login successful!")
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .transition(.move(edge: .top))
            }
            Spacer()
        }
        .background(Color(.systemGroupedBackground))
        .cornerRadius(16)
        .padding()
        .animation(.easeInOut, value: viewModel.errorMessage)
        .animation(.easeInOut, value: viewModel.isLoggedIn)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
} 