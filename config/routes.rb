Bioluminous::Application.routes.draw do
  root 'home#index'
  
  get 'index' => 'home#index'

  scope "games" do
    root to: "games#index", as: :games
    get "gather" => "games#gather", as: :gather
  end
  
  get "rp/libram" => "tabletop#libram", as: :libram

  get 'fos' => 'home#fos'

  get "insults" => 'insults#index'
  post "insults" => "insults#generate"
end
