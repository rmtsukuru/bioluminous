Bioluminous::Application.routes.draw do
  root 'home#index'
  
  get 'index' => 'home#index'

  get 'fos' => 'home#fos'

  get "insults" => 'insults#index'
  post "insults" => "insults#generate"
end
