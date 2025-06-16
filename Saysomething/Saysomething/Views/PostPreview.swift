//This is the post that shown on the home page.
//This version is currently just a basic preview structure of each post
//Not yet connect with Post.swift


import SwiftUI


struct PostPreview: View {
    let post: Post
    @ObservedObject var viewModel: FeedViewModel

    var body: some View {
        VStack {
            HStack(alignment: .center){
                Image(systemName: "heart")
                Text("\(post.resonance) Resonance")
                VStack {
                    Image(systemName: "eye")
                    Text("\(post.view)")  
                }
            }
            Spacer()
            Text(post.title)
                .font(.system(.title2, weight: .bold)) // Combine size and weight
            Image(post.preImg) // Preview image
                .resizable()
                .frame(width: 150, height: 150)

            Spacer()
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(10)  // Fixed: Corrected spelling from 'cornerRadius'
    }
}


