Bioluminous::Application.routes.draw do
  root 'home#index'
  
  get 'index' => 'home#index'

  # TODO Add new controller for this
  get "games/gather/" => "home#gather", as: :gather

  get 'fos' => 'home#fos'

  get "insults" => 'insults#index'
  post "insults" => "insults#generate"
end
