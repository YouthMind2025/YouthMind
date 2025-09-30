import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()
    @FocusState private var focusedField: Field?

    enum Field { case email, otp }

    private var emailValid: Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        let emailPred = NSPredicate(format: "SELF MATCHES %@", emailRegEx)
        return emailPred.evaluate(with: viewModel.email)
    }
    private var otpValid: Bool { viewModel.otp.count == 6 && viewModel.otp.allSatisfy { $0.isNumber } }
    private var canSubmit: Bool { emailValid && otpValid && !viewModel.isLoading }

    var body: some View {
        ZStack {
            LinearGradient(gradient: Gradient(colors: [DesignConstants.ColorPalette.background, Color.white]), startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Brand/Header
                VStack(spacing: 8) {
                    Image(systemName: "bubble.left.and.bubble.right.fill")
                        .font(.system(size: 44))
                        .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                        .shadow(color: DesignConstants.ColorPalette.brandPurple.opacity(0.25), radius: 8, x: 0, y: 8)
                    Text("SaySomething")
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundColor(DesignConstants.ColorPalette.textPrimary)
                    Text("Welcome back. Let’s get you in.")
                        .font(.subheadline)
                        .foregroundColor(DesignConstants.ColorPalette.textSecondary)
                }
                .padding(.top, 24)

                // Card
                VStack(spacing: 16) {
                    // Segmented control
                    Picker("Mode", selection: $viewModel.mode) {
                        ForEach(LoginViewModel.AuthMode.allCases) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .tint(DesignConstants.ColorPalette.brandPurple)

                    // Email
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 10) {
                            Image(systemName: "envelope")
                                .foregroundColor(emailValid || viewModel.email.isEmpty ? .secondary : .red)
                            TextField("Email", text: $viewModel.email)
                                .keyboardType(.emailAddress)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .focused($focusedField, equals: .email)
                                .submitLabel(.next)
                                .onSubmit { focusedField = .otp }
                        }
                        .padding(14)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color(.secondarySystemBackground)))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(!emailValid && !viewModel.email.isEmpty ? Color.red.opacity(0.6) : Color.clear, lineWidth: 1)
                        )
                        if !emailValid && !viewModel.email.isEmpty {
                            Text("Please enter a valid email.")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }

                    // OTP
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 10) {
                            Image(systemName: "number")
                                .foregroundColor(otpValid || viewModel.otp.isEmpty ? .secondary : .red)
                            TextField("6-digit code", text: $viewModel.otp)
                                .keyboardType(.numberPad)
                                .focused($focusedField, equals: .otp)
                        }
                        .padding(14)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color(.secondarySystemBackground)))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(!otpValid && !viewModel.otp.isEmpty ? Color.red.opacity(0.6) : Color.clear, lineWidth: 1)
                        )
                        if !otpValid && !viewModel.otp.isEmpty {
                            Text("OTP must be 6 digits.")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }

                    // Actions
                    HStack(spacing: 12) {
                        Button(action: { viewModel.sendCode() }) {
                            HStack(spacing: 8) {
                                Image(systemName: "paperplane.fill")
                                Text(viewModel.isCodeSent ? (viewModel.resendSecondsRemaining == 0 ? "Resend" : "Resend in \(viewModel.resendSecondsRemaining)s") : "Send Code")
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(DesignConstants.ColorPalette.brandPurple.opacity(viewModel.resendSecondsRemaining == 0 ? 0.12 : 0.08))
                            )
                        }
                        .disabled(!emailValid || viewModel.isLoading || (viewModel.isCodeSent && viewModel.resendSecondsRemaining > 0))

                        Button(action: { viewModel.register() }) {
                            HStack(spacing: 8) {
                                Image(systemName: viewModel.mode == .login ? "arrow.right.circle.fill" : "person.badge.plus.fill")
                                Text(viewModel.mode == .login ? "Login" : "Register")
                                    .bold()
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(
                                LinearGradient(gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple, DesignConstants.ColorPalette.brandPurple.opacity(0.9)]), startPoint: .topLeading, endPoint: .bottomTrailing)
                            )
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: Color.black.opacity(0.1), radius: 3, x: 0, y: 2)
                        }
                        .disabled(!canSubmit)
                    }

                    if viewModel.isLoading {
                        ProgressView().padding(.top, 4)
                    }
                }
                .padding(20)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color.black.opacity(0.06), lineWidth: 1)
                )
                .padding(.horizontal)

                if let error = viewModel.errorMessage {
                    banner(text: error, color: .red)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
                if let success = viewModel.successMessage {
                    banner(text: success, color: .green)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }

                Spacer()
            }
            .padding(.top, 16)
            .animation(.easeInOut, value: viewModel.errorMessage)
            .animation(.easeInOut, value: viewModel.successMessage)
        }
    }

    @ViewBuilder
    private func banner(text: String, color: Color) -> some View {
        HStack(spacing: 12) {
            Image(systemName: color == .red ? "exclamationmark.octagon.fill" : "checkmark.seal.fill")
                .foregroundColor(.white)
            Text(text)
                .foregroundColor(.white)
                .font(.subheadline)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            LoginView()
                .preferredColorScheme(.light)
            LoginView()
                .preferredColorScheme(.dark)
        }
    }
}
