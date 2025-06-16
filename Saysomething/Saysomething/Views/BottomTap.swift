//Bottomtap located on the bottom of the HomePage



import SwiftUI

struct BottomTap: View {
    
    var body: some View {
        TabView {
            HomePage()
                .tabItem {
                    Image(systemName: "house")
                    Text("Home")
                }
            
            YouAIView()
                .tabItem {
                    Image(systemName: "heart")
                    Text("YouAI")
                }
                    
            PostSomethingView()
                .tabItem {
                    Image(systemName: "plus.square")
                    Text("Post")
                }
            
            MessageView()
                .tabItem {
                    Image(systemName: "message")
                    Text("Messages")
                }
                    
                    
            MeView()
                .tabItem {
                    Image(systemName: "person")
                    Text("Me")
                }
            }
            .accentColor(.purple)
    }
}

#Preview {
    BottomTap()
}



