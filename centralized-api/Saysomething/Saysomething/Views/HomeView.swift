import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        VStack {
            Text(viewModel.title)
                .font(.title)
                .padding()
            
            if viewModel.isLoading {
                ProgressView()
            }
            
            Text("Welcome to the main app!")
                .padding()
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
} 