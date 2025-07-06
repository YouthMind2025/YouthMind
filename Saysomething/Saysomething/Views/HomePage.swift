//HomePage.swift
//Implement basic structure of the HomePage
// Including topBar, feed and bottomtap UI.


import SwiftUI

struct HomePage: View {
    @State var currentHome = 0
    
    var body: some View {
        VStack{
            
            NavigationStack{
                ScrollView{
                    HStack{
                        Spacer()
                        VStack{
                            //dummy data
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                        }//end of VStack1 in HStack
                        
                        VStack{
                            //dummy data
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                            PostPreview( resonance: 138, view:200, title: "KDB Left Cold",preImg: "character1")
                        }//end of VStack2 in HStack
                        Spacer()
                    }//end of HStack
                }//nav. stack end
                .navigationTitle(Text("Discovery"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    // First leading item
                    ToolbarItem(placement: .navigationBarLeading) {
                        Image(systemName: "gearshape.fill")
                    }
                    
                    // Second leading item - will appear to the right of the first one
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Image(systemName: "magnifyingglass") // Same icon just for example
                    }//toolbar
                }//toolbar end
            } //scrolll view end
            
            
            BottomTap()
        }//Vstack end
    }
}

#Preview {
    HomePage()
}



