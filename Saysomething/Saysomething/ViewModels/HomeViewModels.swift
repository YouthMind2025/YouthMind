//
//  HomeViewModels.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI

class HomeViewModel: ObservableObject {
    @Published var title: String = "Welcome to Home"
    @Published var isLoading: Bool = false
    
    
}
