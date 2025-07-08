//
//  HomeView.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        NavigationStack {
            VStack(spacing:20) {
                Text(viewModel.title)
                    .font(.largeTitle)
                    .padding()
                
                NavigationLink {
                    DetailView(content: "Resonance")
                } label: {
                Text("Resonance")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                
                NavigationLink {
                    DetailView(content: "Saved")
                } label: {
                Text("Saved")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                
                NavigationLink {
                    DetailView(content: "Shared")
                } label: {
                Text("Shared")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                
                NavigationLink {
                    DetailView(content: "Interactions")
                } label: {
                Text("Interactions")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                
                if viewModel.isLoading {
                    ProgressView("Loading...")
                        .padding()
                }
                // UUID label for testing
                Text("UUID: \(UserIdentityManager.shared.userID)")
                    .font(.footnote)
                    .foregroundColor(.gray)
                    .padding(.top, 20)
            }.navigationTitle("Home Page")
        }
    }
}

struct DetailView: View {
    let content: String
    
    var body: some View {
        VStack {
            // Content
            Text("detail：\(content)")
                .font(.title)
                .padding()
            
            // DESCRIPTION
            Text("This is \(content) page")
                .foregroundColor(.secondary)
            
            Spacer()
        }
        .navigationTitle("detail")
        .navigationBarTitleDisplayMode(.inline)
    }
}


struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
