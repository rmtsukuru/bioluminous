class InsultsController < ApplicationController
  include InsultsHelper

  # GET /insults
  def index
    @insult = fetch_insult
  end

  # POST /insults
  def generate
    if request.xhr?
      render text: fetch_insult
    else
      redirect_to action: 'index'
    end
  end

  private

  def fetch_insult
    insult = []
    insult << insult_list1[Random.rand(insult_list1.size)]
    insult << insult_list2[Random.rand(insult_list2.size)]
    insult << insult_list3[Random.rand(insult_list3.size)]
    insult.join ' '
  end
end
