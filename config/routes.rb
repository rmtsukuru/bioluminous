Bioluminous::Application.routes.draw do
  root 'home#index'
  
  get 'index' => 'home#index'

  get 'fos' => 'home#fos'
end
